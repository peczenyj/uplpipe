use common::sense;
package Uplpipe::Agent;

use Data::Dumper;
use English qw( −no_match_vars );
use Digest::MD5::File;
use Net::Amazon::S3;
use Log::Log4perl;
use Data::Dumper;
use LWP::UserAgent;
use Moose;

our $VERSION = '0.01';

has 'conf' => (is => 'ro', required => 1);

has 'logger' => ( 
	is => 'rw', 
	handles => {
		log_debug => 'debug',
		log_info  => 'info',
		log_warn  => 'warn',
		log_error => 'error',
		log_fatal => 'fatal'
	},
	lazy => 1,
	default => sub { Log::Log4perl::get_logger(__PACKAGE__); }
);

has 's3' => ( 
	is => 'rw', 
	handles => {
		s3_err    => 'err',
		s3_errstr => 'errstr'
	},
	lazy => 1,	
	default => sub { Net::Amazon::S3->new( shift->conf()->{amazon} ) }
);

has 'ua' => (
	is => 'rw',
	handles => {post_to_uplpipe => 'post'},
	lazy => 1,	
	default => sub { LWP::UserAgent->new( %{shift->conf()->{lwp_user_agent}} ) }
);

has 'bucket' => (
	is => 'rw',
	lazy_build => 1, 
	handles => {
		add_file_to_bucket => 'add_key_filename',
		set_acl_to_bucket => 'set_acl',
		head_key_for_key => 'head_key'
	}
);

sub _build_bucket {
	my $self = shift;
	$self->s3->bucket($self->bucketname)
}

has 'bucketname' => (
	is => 'rw',
	lazy => 1,
	default=> sub { shift->conf()->{app}->{bucketname} }
);

has 'uplpipe_url' => ( 
	is => 'rw',
	lazy => 1,
	default => sub { shift->conf()->{app}->{uplpipe_url}}
);

has ['lwp_error', 'some_error'] => ( is => 'rw',  default => '');

before 'process' => sub { shift->clear_errors; };

sub process{
	my ($self,$obj) = @_;
	my $file = $obj->stringify;
	my $keyname = $obj->basename;
	
	$self->log_debug("processing: '$keyname'");
	
	$self->upload_file_if_necessary($keyname,$file)
		and $self->change_acl_to_public_read($keyname)
		and $self->change_download_url_for($keyname)
		and $self->delete_file($file)
		and $self->on_success($file)
		or  $self->on_error($file)
}

sub upload_file_if_necessary {
	my ($self,$keyname,$file) = @_;
	
	$self->_check_file_exist($keyname,$file) 
		or $self->_upload_file_to_s3($keyname,$file)
}

sub _check_file_exist{
	my ($self,$keyname,$file) = @_;
	my $data = $self->head_key_for_key($keyname);
	my $etag = Digest::MD5::File::file_md5_hex($file);
	my $result = exists $data->{etag} && $data->{etag} eq $etag;
	$self->log_debug("file $keyname md5: $etag, data: ", {
		filter => \&Dumper, 
		value => $data
	});	
	
	$result
}

sub _upload_file_to_s3 {
	my ($self,$keyname,$file) = @_;
	$self->log_debug("add $keyname to bucket");
	
	$self->add_file_to_bucket($keyname,$file) 
		and do { 
			my $check = $self->_check_file_exist($keyname,$file);
			$self->some_error("store $keyname in s3 but check file returns false") unless $check;
			$check
		}
}

sub change_acl_to_public_read{
	my ($self,$keyname) = @_;
	$self->log_debug("change acl for $keyname");
		
	$self->set_acl_to_bucket({ key => $keyname, acl_short => 'public-read' });
}

sub change_download_url_for {
	my ($self,$keyname) = @_;
	$self->log_debug("change download url for $keyname");

	my $response = $self->post_to_uplpipe($self->uplpipe_url_for($keyname), url => $self->s3url_for($keyname));	
	$self->lwp_error($response->status_line) unless($response->is_success);
	
	$response->is_success
}

sub uplpipe_url_for {
	my ($self,$keyname) = @_;
	my $url = $self->uplpipe_url;
	$url =~ s/:id/$keyname/;
	$url
}

sub s3url_for {
	my ($self,$keyname) = @_;
	
	sprintf "http://s3.amazonaws.com/%s/%s", $self->bucketname, $keyname
}

sub delete_file {
	my ($self,$file) = @_;
	
	Uplpipe::Agent::unlink($file) 
		or do { 
			$self->some_error($ERRNO); 
			0 
		}
}

sub unlink {
	CORE::unlink(@_);
}

sub on_success {
	my ($self,$file) = @_;
	
	$self->log_info("Success for $file");
	
	1;
}

sub on_error {
	my ($self,$file) = @_;
	
	$self->log_error("one or more errors: ". $self->get_errors);
	
	0;
}

sub get_errors {
	my $self = shift;

	my $s3errs = $self->s3_err ? $self->s3_err . ":" . $self->s3_errstr : "";
	
	join("",$self->lwp_error, $self->some_error , $s3errs)
}

sub clear_errors {
	my $self = shift;
	
	$self->lwp_error("");
	$self->some_error("");
}

__PACKAGE__->meta->make_immutable;
no Moose;

1;
__END__

=head1 NAME

Uplpipe::Agent - Perl extension for uplpipe solution.

=head1 SYNOPSIS

  use Uplpipe::Agent;
  my $agent= Uplpipe::Agent->new(conf => $configuration);
  $agent->process($file); 	

=head1 DESCRIPTION

This agent process one file for uplpipe and store in s3, change the download url in process.

This is a Moose class. Attributes has getter/setter. Only conf is read only.

=head2 CONFIGURATION

Configuration file example, using Config::Tiny in this example

[logging]

log4perl.category.daemon = DEBUG, Screen
log4perl.category.Uplpipe.Agent = DEBUG, Screen
log4perl.appender.Screen = Log::Log4perl::Appender::Screen
log4perl.appender.Screen.layout = SimpleLayout

[amazon]

retry=1

aws_access_key_id=fake_aws_access_key_id
aws_secret_access_key=fake_aws_secret_access_key

 
[app]

bucketname=uplpipe
uplpipe_url=http://localhost:8080/scala/upload/:id/download

[lwp_user_agent]

timeout=1

[daemon]

directory=/tmp/incoming
interval=10


=head1 METHODS

=head2 new 

Constructor of Uplpipe, just configuration object is required.

=head2 process

Process one file (Path::Class::File) in three phases.

First: check in s3 for some key in bucketname and compares the etag/md5, if none match, stores the file. 
Second: change the download url this file in uplpipe using the filename as id.
Final: delete the file from local storage.

return success if all phases return ok - see log file for more information in case of failure

=head2 logger

Attribute for a Log::Log4perl instance - it should be initialized correctly. A simple logger for this agent.

Default: 
	a logger for __PACKAGE__

Handles:
	log_debug => 'debug',
	log_info  => 'info',
	log_warn  => 'warn',
	log_error => 'error',
	log_fatal => 'fatal'

=head2 s3

Attribute for a Net::Amazon::S3 instance, to store and verify files in amazon s3.

Default: 
	new instance with $conf->{amazon} value

Handles:
	s3_err    => 'err',
	s3_errstr => 'errstr'

Provides: 
	bucket attribute

=head2 ua

Attribute for LWP::UserAgent instance, to post new url in uplpipe rest service.

Default: 
	new instance with $conf->{lwp_user_agent} value

Handles:
	post_to_uplpipe => 'post
	
=head2 bucket

A New::Amazon::S3::Bucket instance, to work with buckets in amazon.

Default:
	$self->s3->bucket($self->bucketname)

Handles:
	add_file_to_bucket => 'add_key_filename',
	set_acl_to_bucket => 'set_acl',
	head_key_for_key => 'head_key'	
	
=head2  bucketname

Attribute for bucket name in s3.

Default:
	$conf->{app}->{bucketname}
	
=head2 uplpipe_url	
	
Attribute for the uplpipe url template to change the url in uplpipe rest service. For example

http://server/path/to/resource/:id/part

where :id will be change to the id/keyname in uplpipe

Default:
	$conf->{app}->{uplpipe_url}
	
=head2 EXPORT

None by default.

=head1 AUTHOR

peczenyj, E<lt>tiago.peczenyj@gmail.comE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2011 by peczenyj

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.12.3 or,
at your option, any later version of Perl 5 you may have available.


=cut
