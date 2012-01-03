use Test::More;
use Test::MockObject;
use Test::MockModule;

use Uplpipe::Agent;

my $conf = {
	app => {
		bucketname=> 'uplpipe',
		uplpipe_url=> 'http://localhost:8080/scala/upload/:id/download',		
	}
};

subtest 'basic' => sub {
	my $agent = Uplpipe::Agent->new( conf => $conf);
	ok($agent, "should be true");
	isa_ok($agent, "Uplpipe::Agent", "should be an instance of Uplpipe::Agent");
};

sub get_s3_valid {
	my $s3     = Test::MockObject->new();
	$s3->set_always('err',undef);
	$s3->set_always('errstr',undef);
	my $bucket = Test::MockObject->new();
	$s3->set_always('bucket',$bucket);
	$bucket->set_series('head_key',undef,{ etag => 'md5'});
	$bucket->set_true('set_acl', 'add_key_filename');
	
	$s3
}

sub get_logger_valid {
	my $logger = Test::MockObject->new();	
	$logger->set_true('debug','info','error');
	
	$logger
}

sub get_file {
	my $file   = Test::MockObject->new();
	$file->set_always('stringify','/not/exist/a');
	$file->set_always('basename','a');
	
	$file
}

sub ok_s3 {
	my $s3 = shift;
	$s3->called_ok('bucket','should get the bucket object');
	my $bucket = $s3->bucket;
	$bucket->called_ok('head_key','should call head_key to verify s3 properties');
	$bucket->called_ok('set_acl','should call set_acl to modify acls');
	$bucket->called_ok('add_key_filename','should call add key filename');
}
sub ok_logger {
	my $logger = shift;
	$logger->called_pos_ok(-1,'info','should call logger->info at the end');
	$logger->called_args_pos_is(-1,2,'Success for /not/exist/a','should log the success for file');
};

subtest "happy path" => sub {
	my $s3     = get_s3_valid;
	my $logger = get_logger_valid;
	my $file   = get_file;
	
	my $agent  = Uplpipe::Agent->new( 
		conf   => $conf,
		s3     => $s3,
		logger => $logger,
	);
	
	my $md5module = new Test::MockModule('Digest::MD5::File');
	$md5module->mock('file_md5_hex', sub { 'md5'; });

	my $module = new Test::MockModule('Uplpipe::Agent');
	$module->mock('unlink', sub { 1; });	
	
	my $result = $agent->process($file);
	
	ok($result, "process status should be true");
	ok_s3($s3);
	ok_logger($logger);
};

sub get_s3_different_etags {
	my $s3     = Test::MockObject->new();
	$s3->set_always('err',undef);
	$s3->set_always('errstr',undef);
	my $bucket = Test::MockObject->new();
	$s3->set_always('bucket',$bucket);
	$bucket->set_series('head_key',{etag => 'md4'},{ etag => 'md5'});
	$bucket->set_true('set_acl', 'add_key_filename');
	
	$s3
}

subtest "upload file if file in s3 was different" => sub {
	my $s3     = get_s3_different_etags;
	my $logger = get_logger_valid;
	my $file   = get_file;
	
	my $agent  = Uplpipe::Agent->new( 
		conf   => $conf,
		s3     => $s3,
		logger => $logger,
	);
	
	my $md5module = new Test::MockModule('Digest::MD5::File');
	$md5module->mock('file_md5_hex', sub { 'md5'; });

	my $module = new Test::MockModule('Uplpipe::Agent');
	$module->mock('unlink', sub { 1; });	
	
	my $result = $agent->process($file);
	
	ok($result, "process status should be true");
	ok_s3($s3);
	ok_logger($logger);
};


sub get_s3_file_already_uploaded {
	my $s3     = Test::MockObject->new();
	$s3->set_always('err',undef);
	$s3->set_always('errstr',undef);
	my $bucket = Test::MockObject->new();
	$s3->set_always('bucket',$bucket);
	$bucket->set_series('head_key',{ etag => 'md5'});
	$bucket->set_true('set_acl', 'add_key_filename');
	
	$s3
}

sub ok_s3_file_already_uploaded {
	my $s3 = shift;
	$s3->called_ok('bucket','should get the bucket object');
	my $bucket = $s3->bucket;
	$bucket->called_ok('head_key','should call head_key to verify s3 properties');
	$bucket->called_ok('set_acl','should call set_acl to modify acls');
	ok(! $bucket->called('add_key_filename'), 'should not upload the file in s3');
}

subtest "file in s3, just do the rest" => sub {
	my $s3     = get_s3_file_already_uploaded;
	my $logger = get_logger_valid;
	my $file   = get_file;
	
	my $agent  = Uplpipe::Agent->new( 
		conf   => $conf,
		s3     => $s3,
		logger => $logger,
	);
	
	my $md5module = new Test::MockModule('Digest::MD5::File');
	$md5module->mock('file_md5_hex', sub { 'md5'; });

	my $module = new Test::MockModule('Uplpipe::Agent');
	$module->mock('unlink', sub { 1; });	
	
	my $result = $agent->process($file);
	
	ok($result, "process status should be true");
	ok_s3_file_already_uploaded($s3);
	ok_logger($logger);
};

sub ok_logger_error{
	my $logger = shift;
	my $msg = shift;
	$logger->called_pos_ok(-1,'error','should call logger->info at the end');
	$logger->called_args_pos_is(-1,2,$msg,'should log the error in log');	
}

subtest "failure in unlink file" => sub {
	my $s3     = get_s3_valid;
	my $logger = get_logger_valid;
	my $file   = get_file;
	
	my $agent  = Uplpipe::Agent->new( 
		conf   => $conf,
		s3     => $s3,
		logger => $logger,
	);
	
	my $md5module = new Test::MockModule('Digest::MD5::File');
	$md5module->mock('file_md5_hex', sub { 'md5'; });	
	
	my $result = $agent->process($file);
	
	ok(! $result, "process status should be true");
	ok_s3($s3);
	ok_logger_error($logger, 'one or more errors for file /not/exist/a: No such file or directory');	
};


sub get_s3_invalid {
	my $s3     = Test::MockObject->new();
	$s3->set_always('err','>err');
	$s3->set_always('errstr','>errstr');
	my $bucket = Test::MockObject->new();
	$s3->set_always('bucket',$bucket);
	$bucket->set_series('head_key',undef,{ etag => 'md5'});
	$bucket->set_true('add_key_filename');
	$bucket->set_false('set_acl');
	
	$s3	
}

sub ok_s3_invalid{
	my $s3 = shift;
	$s3->called_ok('bucket','should get the bucket object');
	my $bucket = $s3->bucket;
	$bucket->called_ok('head_key','should call head_key to verify s3 properties');
	$bucket->called_ok('set_acl','should call set_acl to modify acls');
	$bucket->called_ok('add_key_filename','should call add key filename');
}

subtest "failure in update acl in s3" => sub {
	my $s3     = get_s3_invalid;
	my $logger = get_logger_valid;
	my $file   = get_file;
	
	my $agent  = Uplpipe::Agent->new( 
		conf   => $conf,
		s3     => $s3,
		logger => $logger,
	);
	
	my $md5module = new Test::MockModule('Digest::MD5::File');
	$md5module->mock('file_md5_hex', sub { 'md5'; });	
	
	my $result = $agent->process($file);
	
	ok(! $result, "process status should be true");
	ok_s3_invalid($s3);
	ok_logger_error($logger, 'one or more errors for file /not/exist/a: >err:>errstr');	
};

sub get_s3_invalid2{
	my $s3     = Test::MockObject->new();
	$s3->set_always('err',undef);
	$s3->set_always('errstr',undef);
	my $bucket = Test::MockObject->new();
	$s3->set_always('bucket',$bucket);
	$bucket->set_series('head_key',undef,{ etag => 'md6'});
	$bucket->set_true('set_acl', 'add_key_filename');
	
	$s3	
}
sub ok_s3_invalid2 {
	my $s3 = shift;
	$s3->called_ok('bucket','should get the bucket object');
	my $bucket = $s3->bucket;
	$bucket->called_ok('head_key','should call head_key to verify s3 properties');
	ok(! $bucket->called('set_acl'), 'should not call set_acl');
	$bucket->called_ok('add_key_filename','should call add key filename');
}

subtest "upload successfull but head_key shows different etag/md5" => sub {
	my $s3     = get_s3_invalid2;
	my $logger = get_logger_valid;
	my $file   = get_file;
	
	my $agent  = Uplpipe::Agent->new( 
		conf   => $conf,
		s3     => $s3,
		logger => $logger,
	);
	
	my $md5module = new Test::MockModule('Digest::MD5::File');
	$md5module->mock('file_md5_hex', sub { 'md5'; });

	my $module = new Test::MockModule('Uplpipe::Agent');
	$module->mock('unlink', sub { 1; });	
	
	my $result = $agent->process($file);
	
	ok(! $result, "process status should be false");
	ok_s3_invalid2($s3);
	ok_logger_error($logger, 'one or more errors for file /not/exist/a: store a in s3 but check file returns false');
};

sub get_s3_invalid3{
	my $s3     = Test::MockObject->new();
	$s3->set_always('err', '>err');
	$s3->set_always('errstr','>errstr');
	my $bucket = Test::MockObject->new();
	$s3->set_always('bucket',$bucket);
	$bucket->set_series('head_key',undef);
	$bucket->set_false('add_key_filename');
	
	$s3
}

subtest "failure to upload to s3" => sub {
	my $s3     = get_s3_invalid3;
	my $logger = get_logger_valid;
	my $file   = get_file;
	
	my $agent  = Uplpipe::Agent->new( 
		conf   => $conf,
		s3     => $s3,
		logger => $logger,
	);
	
	my $md5module = new Test::MockModule('Digest::MD5::File');
	$md5module->mock('file_md5_hex', sub { 'md5'; });

	my $module = new Test::MockModule('Uplpipe::Agent');
	$module->mock('unlink', sub { 1; });	
	
	my $result = $agent->process($file);
	
	ok(! $result, "process status should be false");
	ok_s3_invalid2($s3);
	ok_logger_error($logger, 'one or more errors for file /not/exist/a: >err:>errstr');	
};

done_testing();

__END__

coverage:
---------

---------------------------- ------ ------ ------ ------ ------ ------ ------
File                           stmt   bran   cond    sub    pod   time  total
---------------------------- ------ ------ ------ ------ ------ ------ ------
blib/lib/Uplpipe/Agent.pm     100.0  100.0  100.0  100.0    n/a  100.0  100.0
Total                         100.0  100.0  100.0  100.0    n/a  100.0  100.0
---------------------------- ------ ------ ------ ------ ------ ------ ------

unit test result:
-----------------

PERL_DL_NONLAZY=1 /opt/local/bin/perl "-MExtUtils::Command::MM" "-e" "test_harness(1, 'blib/lib', 'blib/arch')" t/*.t
t/01-load.t .... 
1..2
ok 1 - use Uplpipe::Agent;
ok 2 - require Uplpipe::Agent;
ok
t/02-basics.t .. 
    ok 1 - should be true
    ok 2 - should be an instance of Uplpipe::Agent isa Uplpipe::Agent
    1..2
ok 1 - basic
    ok 1 - process status should be true
    ok 2 - should get the bucket object
    ok 3 - should call head_key to verify s3 properties
    ok 4 - should call set_acl to modify acls
    ok 5 - should call add key filename
    ok 6 - post method should be called
    ok 7 - should post url to uplpipe
    ok 8 - should pass url key
    ok 9 - should pass url value
    ok 10 - post success should be verified
    ok 11 - should call logger->info at the end
    ok 12 - should log the success for file
    1..12
ok 2 - happy path
    ok 1 - process status should be true
    ok 2 - should get the bucket object
    ok 3 - should call head_key to verify s3 properties
    ok 4 - should call set_acl to modify acls
    ok 5 - should call add key filename
    ok 6 - post method should be called
    ok 7 - should post url to uplpipe
    ok 8 - should pass url key
    ok 9 - should pass url value
    ok 10 - post success should be verified
    ok 11 - should call logger->info at the end
    ok 12 - should log the success for file
    1..12
ok 3 - upload file if file in s3 was different
    ok 1 - process status should be true
    ok 2 - should get the bucket object
    ok 3 - should call head_key to verify s3 properties
    ok 4 - should call set_acl to modify acls
    ok 5 - should not upload the file in s3
    ok 6 - post method should be called
    ok 7 - should post url to uplpipe
    ok 8 - should pass url key
    ok 9 - should pass url value
    ok 10 - post success should be verified
    ok 11 - should call logger->info at the end
    ok 12 - should log the success for file
    1..12
ok 4 - file in s3, just do the rest
    ok 1 - process status should be true
    ok 2 - should get the bucket object
    ok 3 - should call head_key to verify s3 properties
    ok 4 - should call set_acl to modify acls
    ok 5 - should call add key filename
    ok 6 - post method should be called
    ok 7 - should post url to uplpipe
    ok 8 - should pass url key
    ok 9 - should pass url value
    ok 10 - post success should be verified
    ok 11 - should call logger->info at the end
    ok 12 - should log the error in log
    1..12
ok 5 - failure in unlink file
    ok 1 - process status should be true
    ok 2 - should get the bucket object
    ok 3 - should call head_key to verify s3 properties
    ok 4 - should call set_acl to modify acls
    ok 5 - should call add key filename
    ok 6 - post method should be called
    ok 7 - should post url to uplpipe
    ok 8 - should pass url key
    ok 9 - should pass url value
    ok 10 - post success should be verified
    ok 11 - should verify the status line
    ok 12 - should call logger->info at the end
    ok 13 - should log the error in log
    1..13
ok 6 - failure in update rest api
    ok 1 - process status should be true
    ok 2 - should get the bucket object
    ok 3 - should call head_key to verify s3 properties
    ok 4 - should call set_acl to modify acls
    ok 5 - should call add key filename
    ok 6 - post method should NOT be called
    ok 7 - should call logger->info at the end
    ok 8 - should log the error in log
    1..8
ok 7 - failure in update acl in s3
    ok 1 - process status should be false
    ok 2 - should get the bucket object
    ok 3 - should call head_key to verify s3 properties
    ok 4 - should not call set_acl
    ok 5 - should call add key filename
    ok 6 - post should no be called
    ok 7 - should call logger->info at the end
    ok 8 - should log the error in log
    1..8
ok 8 - upload successfull but head_key shows different etag/md5
    ok 1 - process status should be false
    ok 2 - should get the bucket object
    ok 3 - should call head_key to verify s3 properties
    ok 4 - should not call set_acl
    ok 5 - should call add key filename
    ok 6 - post should no be called
    ok 7 - should call logger->info at the end
    ok 8 - should log the error in log
    1..8
ok 9 - failure to upload to s3
1..9
ok
All tests successful.
Files=2, Tests=11,  1 wallclock secs ( 0.04 usr  0.01 sys +  1.47 cusr  0.09 csys =  1.61 CPU)
Result: PASS
