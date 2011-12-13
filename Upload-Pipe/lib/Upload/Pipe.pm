use common::sense;
package Upload::Pipe;

use AnyEvent;
use AnyEvent::HTTPD;
use MIME::Types;
use JSON;

my $mt = MIME::Types->new();

#
# Proof of concept using AnyEvent::HTTPD
# AnyEvent is a framework to do event-based programming and provides non-blocking I/O
# AnyEvent::HTTP is a simple lightweight event based web (application) server
#  with connection_class I can pass my own connection class.
#  it is very important because the AnyEvent::HTTPD::HTTPConnection store and parse
#  the entire body, and I can extend this class easily
#

my $httpd = AnyEvent::HTTPD->new (
	port => 5000,
#	connection_class => 'AnyEvent::HTTPD::HTTPConnection2' -- or other name
);

$httpd->reg_cb (
   '/upload' => sub {
      my ($httpd, $req) = @_;

      $req->respond ({ 
		content => [
			$mt->mimeTypeOf('html'), 
			"OK"
		]
	   });
   },
   '/status' => sub {
      my ($httpd, $req) = @_;
      $req->respond ({ 
		content => [
			$mt->mimeTypeOf('js'), 
			encode_json { percentage => 0 }
		]
	});
   },
);


#$httpd->run;	

1;
