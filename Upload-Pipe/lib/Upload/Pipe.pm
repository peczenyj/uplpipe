use strict;
use warnings;
package Upload::Pipe;

use AnyEvent;
use AnyEvent::HTTPD;
use MIME::Types;
use JSON;

my $mt = MIME::Types->new();

my $httpd = AnyEvent::HTTPD->new (
	port => 5000,
#	connection_class => 'AnyEvent::HTTPD::HTTPConnection2'
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


$httpd->run;	

1;
