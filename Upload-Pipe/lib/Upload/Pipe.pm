use common::sense;

package AnyEvent::HTTPD::HTTPConnection2;

use parent 'AnyEvent::HTTPD::HTTPConnection';

our $DB = {};
use constant { CHUNK => 8196 };

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
		
         push @{$self->{last_header}}, $hdr;

         if (defined $hdr->{'content-length'}) {
			
			$self->{hdl}->unshift_read(  $self->strategy_for_content_type($hdr)  );
			
         } else {
	
            $self->handle_request (@{$self->{last_header}});

         }
      }
   );
}

sub strategy_for_content_type {
	my ($self, $hdr) = @_;
	
	my ($ctype, $bound) = AnyEvent::HTTPD::HTTPConnection::_content_type_boundary ($hdr->{'content-type'});
	my $buffer = "";

	if ($ctype eq 'multipart/form-data') {  #      $cont = $self->decode_multipart ($cont, $bound);
		my $buffer = "";
		my $cl     = $hdr->{'content-length'};
		my $url    = $self->{last_header}->[1];
		
		
		$DB->{$url} = {total => $cl,remaining => $cl};
		
		return ( chunk =>  (($cl > CHUNK)? CHUNK : $cl),
				$self->read_data_using_chunks_cb($buffer,$cl,$url)
				);
	} 
	
	(chunk => $hdr->{'content-length'}, 
		sub {
       		my ($hdl, $data) = @_;
       		$self->handle_request (@{$self->{last_header}}, $data);
    	})
}

sub read_data_using_chunks_cb {
	my ($self,$buffer,$cl,$url) = @_;
	
	# weaken self ?
	
	my $f; $f = sub {
		my($obj, $data) = @_;
	
		$buffer .= $data; 
		$cl -= length $data; 
		$DB->{$url}->{remaining} = $cl;
		
		AE::log alert => "percentage for $url : $DB->{$url}->{remaining}";
		if($cl > 0){
			$obj->unshift_read( chunk => (($cl > CHUNK)? CHUNK : $cl), $f); 
		} else {	
			$self->my_handle_request (@{$self->{last_header}}, $buffer);
		}
	};
	
	$f
}

sub my_handle_request{
	my $self = shift;
	
	$self->handle_request(@_)
}

1;

package Upload::Pipe;

use AnyEvent;
use AnyEvent::HTTPD;
use JSON;
use MIME::Types;
use Data::Dumper;
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
	
	use Data::Printer;
	p $req->parm('f');
	
	$req->respond ({ 
		content => [
			"text/html", 
			'ok'
		]
	});
	$httpd->stop_request
}

sub status_controller{
	my ($httpd, $req) = @_;

	my $url = $req->url->as_string;
	
	$url =~ s/status/upload/;
	
	my $method = $req->method;
	
	AE::log alert => "status :: para $method $url";
	
	return unless exists $AnyEvent::HTTPD::HTTPConnection2::DB->{$url};
	
	my $data = $AnyEvent::HTTPD::HTTPConnection2::DB->{$url};
	my $progress = 1.0 - $data->{remaining}/$data->{total};
	
	$req->respond ({ 
		content => [
			'application/json', 
			to_json({ progress => $progress})
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
		'/submit'      => \&submit_controller,
		'/'            => \&index_controller,
		''             => \&send404,
	);
	
	AE::log alert => "iniciando o servidor";
	$httpd->run;
}	

use base 'Exporter';
our @EXPORT = qw(run);

1;
