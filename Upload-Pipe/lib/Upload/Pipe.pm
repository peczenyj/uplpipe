use common::sense;

package AnyEvent::HTTPD::HTTPConnection2;

use parent 'AnyEvent::HTTPD::HTTPConnection';

our $percentage = {};

sub Y{ # Y combinator, maybe it is not necessary...
	my ($f) = @_; 
	sub {
		my ($x) = @_; $x->($x)
	}->(sub {
		my ($y) = @_; 
		$f->(sub { $y->($y)->(@_)})
	})
};

#
# Problem:
# the problem here is: i can recieve multiple uploads but the
# AnyEvent:AnyEvent::HTTPD::HTTPConnection::handle_request phase
# call the decode_multipart method who parses the entire body 
# as a single string using regular expression and backreferences
# for large uploads (> 1MB) it is terrible and block other requests
#
# Solution: 
# parse the entire body and save the file async.
#
sub push_header {
   my ($self, $hdl) = @_;

   $self->{hdl}->unshift_read (line =>
      qr{(?<![^\012])\015?\012}o,
      sub {
         my ($hdl, $data) = @_;
         my $hdr = AnyEvent::HTTPD::HTTPConnection::_parse_headers ($data);

         unless (defined $hdr) {
            $self->error (599 => "garbled headers");
         }
		
		 my $url = $self->{last_header}->[1];

         push @{$self->{last_header}}, $hdr;

         if (defined $hdr->{'content-length'}) {

			my $max;
			my $buffer = "";
			my $chunk = 8196;
			my $cl = $max = $hdr->{'content-length'};
			
			$percentage->{$url} = 0;
			$self->{hdl}->unshift_read( chunk => (($cl > $chunk)? $chunk : $cl) , Y(sub {	
					my $f = shift;
					sub {
						my($obj, $data) = @_;
					
						$buffer .= $data; 
						$cl -= length $data; 
						$percentage->{$url} = (100 * ($max-$cl)/$max);

						AE::log alert => "percentage for $url : $percentage->{$url}";
						if($cl > 0){
							$obj->unshift_read( chunk => (($cl > $chunk)? $chunk : $cl), $f); 
						} else {	
							$self->handle_request (@{$self->{last_header}}, $buffer);
						}
					}
				})
			);
			
         } else {
            $self->handle_request (@{$self->{last_header}});
         }
      }
   );
}

1;

1;

package Upload::Pipe;

use AnyEvent;
use AnyEvent::HTTPD;
use JSON;
use MIME::Types;

#
# Proof of concept using AnyEvent::HTTPD
# AnyEvent is a framework to do event-based programming and provides non-blocking I/O
# AnyEvent::HTTP is a simple lightweight event based web (application) server
#  with connection_class I can pass my own connection class.
#  it is very important because the AnyEvent::HTTPD::HTTPConnection store and parse
#  the entire body, and I can extend this class easily
#

sub upload_controller{
	my ($httpd, $req) = @_;
	
	my $url = $req->url->as_string;
	my $method = $req->method;
	AE::log alert => "Upload :: para $method $url";
	
	$req->respond ({ 
		content => [
			"text/html", 
			'ok'
		]
	});
	$httpd->stop_request
}

my $x=0;
sub status_controller{
	my ($httpd, $req) = @_;

	my $url = $req->url->as_string;
	my $method = $req->method;
	AE::log alert => "status :: para $method $url";
	
	$req->respond ({ 
		content => [
			MIME::Types::by_suffix("a.json")->[0], 
			to_json({ progress => $x+=10 })
		]
	});
	
	$httpd->stop_request
}

sub send404 {
	my ($httpd, $req) = @_; 
	my $url = $req->url->as_string;
	my $method = $req->method;
	AE::log alert => "Enviando 404 para $method $url";
	
	$req->respond(
		[404, 'not found', { 'Content-Type' => 'text/plain' }, 'not found']
	);
	$httpd->stop_request	
}

sub desc_controller{ # not finished yet
	my ($httpd, $req) = @_;
	
	my $filepath = $req->parm('remote-file');
	my $description = $req->parm('description');
	
	
	$req->respond ({ content => ['text/html', <<EOT ] });
	<html>
		<body>
			<p>file <a href="$filepath">link</a>!</p><cite>$description</cite>
		</body>
	</html>
EOT

	$httpd->stop_request
}

sub index_controller {
	my ($httpd, $req) = @_;
	AE::log alert => "request para o index !";
	$req->respond ({ content => ['text/html', <<'EOT'  ] });
<html>
<head><title>uplpipe</title>
<script 
src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" 
type="text/javascript" language="javascript"></script>
</head>
<body>
<h1>uplpipe</h1>
<div id="uplpipe1">
<form id="uplpipe_form" method="post" enctype="multipart/form-data" action="#">
<input type="file" name="f" id="uplpipe_f">
</form>
</div>
</body>
</html>	
EOT
	
	$httpd->stop_request 
}

sub run{
	my $httpd = AnyEvent::HTTPD->new (
		port => 5000,
		connection_class => 'AnyEvent::HTTPD::HTTPConnection2' # need a better name...
	);

	$httpd->reg_cb (
		'/upload'      => \&upload_controller,
		'/status'      => \&status_controller,
#		'/submit'      => \&submit_controller,
		'/'            => \&index_controller,
		''             => \&send404,
	);
	
	AE::log alert => "iniciando o servidor";
	$httpd->run;
}	

use base 'Exporter';
our @EXPORT = qw(run);

1;
