use common::sense;
use Uplpipe::Agent;
use Daemon::Generic;
use Config::Tiny;
use POE;
use POE::Component::DirWatch;
use Log::Log4perl;

my $conf;
my $agent;
my $logger;

sub gd_preconfig{
	my ($self) = @_;
	$conf = Config::Tiny->read($self->{configfile});

	Log::Log4perl->init($conf->{logging});
	$logger = Log::Log4perl->get_logger('daemon');
	$agent = Uplpipe::Agent->new(conf => $conf);

	return ();
}

sub gd_run {
	$logger->info("SERVER START");

	my $watcher = POE::Component::DirWatch->new(
		alias      => 'dirwatch',
		directory  =>  $conf->{daemon}->{directory},
		filter     => sub { $_[0] !~ /^incoming.*$/ },
		file_callback => sub{ 
			my $file = shift; $agent->process($file) 
		},
		interval   => $conf->{daemon}->{interval},
	);

	$poe_kernel->run;
	$logger->info("SERVER STOP");
}

sub gd_quit_event{
	$logger->info("SERVER STOPPING...");
	$poe_kernel->call(dirwatch_test => 'shutdown');
}

sub gd_reconfig_event{
	$logger->info("SERVER RECONFIGURING...");
	Daemon::Generic::gd_reconfig_event(@_);
}

newdaemon(
	progname        => 'uplpipe',
	pidfile         => '/var/run/uplpipe.pid',
	configfile      => '/etc/uplpipe.conf',
);
