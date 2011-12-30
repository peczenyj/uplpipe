package Uplpipe::Agent;

our $VERSION = '0.01';

use common::sense;
use Config::Tiny;
use Log::Log4perl;
use File::Copy::Vigilant;
use POE::Component::DirWatch;

my $conf = Config::Tiny->read( 'amazon.conf' );

Log::Log4perl->init( $conf->{logging} );

my $directories = $conf->{directories}; 

my $s3 = Net::Amazon::S3->new( $conf->{aws} ); 

my $bucket = $s3->bucket($conf->{bucket}->{name});

my $rest = $conf->{uplpipe}->{rest};

my $logger = Log::Log4perl::get_logger(__PACKAGE__);

my $dirwatch = POE::Component::DirWatch::New->new({
	alias => 'dirwatch',
	directory => $directories->{indir},
	file_callback => \&_move_to_s3_and_update_rest_service,
	interval => 1,
});

sub _move_to_s3_and_update_rest_service{
	my $file = shift;
	$logger->debug("recieve " . $file->basename);
	# copy to workdir       - File::Copy::Vigilant
	# store in s3           - Net::Amazon::S3
	# update in rest client - LWP
	# delete                - just unlink
}

sub run {
	$poe_kernel->run;
}

1;
__END__
# Below is stub documentation for your module. You'd better edit it!

=head1 NAME

Uplpipe::Agent - Perl extension for blah blah blah

=head1 SYNOPSIS

  use Uplpipe::Agent;
  blah blah blah

=head1 DESCRIPTION

Stub documentation for Uplpipe::Agent, created by h2xs. It looks like the
author of the extension was negligent enough to leave the stub
unedited.

Blah blah blah.

=head2 EXPORT

None by default.



=head1 SEE ALSO

Mention other useful documentation such as the documentation of
related modules or operating system documentation (such as man pages
in UNIX), or any relevant external documentation such as RFCs or
standards.

If you have a mailing list set up for your module, mention it here.

If you have a web site set up for your module, mention it here.

=head1 AUTHOR

peczenyj, E<lt>tiago.peczenyj@gmail.comE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2011 by peczenyj

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.12.3 or,
at your option, any later version of Perl 5 you may have available.


=cut
