#!/usr/bin/perl

## ini2db.pl
#  build a SQLITE db file from an INI file
#
#  example usage:
#    perl ini2db.pl json.ini
#  this creates a json.db file, that can be used with JIDL


use strict;
use warnings;

use DBI;

### initialization
open my $fh, '<', $ARGV[0] or die "cannot read '$ARGV[0]' $!";

# the configuration data
my %data;

### read the contents of the INI file
my $section = '';

while ( my $line = <$fh> ) {
  chomp $line;
  
  # drop comments
  while ( $line =~ m/(.*)#/ ) {
  $line = $1;
  }
  
  if ( $line =~ m/^\s*\[([\w:<-]+)\].*/ ) {
    $section = $1;
  
  $data{$section} = "\n";
  } elsif ( $line =~ m/(.*)=(.*)/ ) {
  my $keyword = $1;
  my $value = $2;
  
  $keyword =~ s/\s//g;
  $value =~ s/\s//g;
  
  die "Parameter without a section" if $section eq "";
  $data{$section} .= "$keyword=$value\n";
  }
}

close $fh;

### write the db file
my $dbfile = $ARGV[0];
if ( $dbfile =~ m/(.*)\.ini/ ) {
  $dbfile = $1;
}
$dbfile = $dbfile . ".db";
my $dsn      = "dbi:SQLite:dbname=$dbfile";
my $dbh = DBI->connect($dsn, '', '', {
   PrintError       => 0,
   RaiseError       => 1,
   AutoCommit       => 1
});

# create the JIDL Diagnostics table
my $sql = <<'END_SQL_Diag';
CREATE TABLE "JIDL Diagnostics" (
  TIMESTAMP  TEXT PRIMARY KEY,
  MESSAGE   TEXT
)
END_SQL_Diag
 
$dbh->do($sql);

# create the JIDL Configuration table
$sql = <<'END_SQL_Conf';
CREATE TABLE "JIDL Configuration" (
  ID  TEXT PRIMARY KEY,
  DATA  TEXT
)
END_SQL_Conf
 
$dbh->do($sql);

# insert all the connections
foreach my $key ( keys %data ) { 
  unless ( $key =~ /::/ ) {
  #print "[$key]".$data{$key};
  $dbh->do( 'INSERT INTO "JIDL Configuration" (id, data) VALUES (?, ?)',
        undef,
        $key, "[$key]".$data{$key} );
  
  $dbh->do( "CREATE TABLE $key (TIMESTAMP TEXT PRIMARY KEY)" );
  }
}

# insert all the tag readers
foreach my $key ( keys %data ) {
  if ( $key =~ /<-/ ) {
  next;
  }
  
  if ( $key =~ /::/ ) {
  #print "[$key]".$data{$key};
  $dbh->do( 'INSERT INTO "JIDL Configuration" (id, data) VALUES (?, ?)',
        undef,
        $key, "[$key]".$data{$key} );
        
  my ($var, $conn) = split( /::/, $key );
  my $type = get_type($data{$key});
  
  if ($type =~ m/BOOLEAN/) {
    $type = 'NUMERIC';
  } elsif ($type =~ m/INTEGER/ || 
           $type =~ m/DOUBLE_INTEGER/ ||
           $type =~ m/BYTE/ ||
           $type =~ m/WORD/) {
    $type = 'INTEGER';
  } elsif ($type =~ m/FLOAT/ || 
           $type =~ m/REAL/) {
    $type = 'REAL';
  } elsif ($type =~ m/TEXT/) {
    $type = 'TEXT';
  }
        
    $dbh->do( "ALTER TABLE $conn ADD COLUMN $var $type" );
  }
}

# insert all the tag writers
foreach my $key ( keys %data ) {
  if ( $key =~ /<-/ ) {
  #print "[$key]".$data{$key};
  $dbh->do( 'INSERT INTO "JIDL Configuration" (id, data) VALUES (?, ?)',
        undef,
        $key, "[$key]".$data{$key} );
  }
}

$dbh->disconnect;

sub get_type {
  my $text = shift;
  my @lines = split( /\n/, $text );
  
  foreach my $line ( @lines ) {
  if ( $line =~ m/^type/ ) {
    my @tokens = split( /=/, $line );
    return $tokens[1];
  }
  } 
}
