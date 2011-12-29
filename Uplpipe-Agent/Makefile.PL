use 5.012003;
use ExtUtils::MakeMaker;
# See lib/ExtUtils/MakeMaker.pm for details of how to influence
# the contents of the Makefile that is written.
WriteMakefile(
    NAME              => 'Uplpipe::Agent',
    VERSION_FROM      => 'lib/Uplpipe/Agent.pm', # finds $VERSION
    PREREQ_PM         => {
		"common::sense" => '3.4',
		"Config::Tiny" => '2.14',
		"Log::Log4perl" => '1.34',
		"App::Rad" => '1.04',
		"Net::Amazon::S3" => '0.56',
		"LWP" => '6.03',
		"File::Copy::Vigilant" => '1.0.1',
		"POE::Component::DirWatch" => '0.3',
}, 
    ($] >= 5.005 ?     ## Add these new keywords supported since 5.005
      (ABSTRACT_FROM  => 'lib/Uplpipe/Agent.pm', # retrieve abstract from module
       AUTHOR         => 'peczenyj <tiago.peczenyj@gmail.com>') : ()),
);
