#!/usr/bin/env perl

use strict;
use warnings;

use DBI;
use File::Spec;
use FindBin;

my $root_dir = File::Spec->catdir( $FindBin::Bin, '..' );
my $db_path  = $ENV{FORK_DB_PATH}
  || File::Spec->catfile( $root_dir, 'fork.db' );
my $migrations_dir = $ENV{FORK_MIGRATIONS_DIR}
  || File::Spec->catdir( $root_dir, 'migrations' );

die "Migrations directory not found: $migrations_dir\n"
  if !-d $migrations_dir;

my $dbh = DBI->connect(
    "dbi:SQLite:dbname=$db_path",
    q{}, q{},
    {
        RaiseError => 1,
        PrintError => 0,
        AutoCommit => 1,
    }
);

$dbh->do('PRAGMA foreign_keys = ON');
$dbh->do(
    q{
        CREATE TABLE IF NOT EXISTS schema_migrations (
            filename TEXT PRIMARY KEY,
            applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
    }
);

opendir my $dh, $migrations_dir
  or die "Unable to open migrations directory $migrations_dir: $!\n";

my @migrations =
  sort grep { /\.sql\z/ && -f File::Spec->catfile( $migrations_dir, $_ ) }
  readdir $dh;

closedir $dh;

my %applied = map { $_ => 1 }
  @{ $dbh->selectcol_arrayref('SELECT filename FROM schema_migrations') };

my @pending = grep { !$applied{$_} } @migrations;

if ( !@pending ) {
    print "No pending migrations for $db_path\n";
    exit 0;
}

for my $filename (@pending) {
    my $path       = File::Spec->catfile( $migrations_dir, $filename );
    my $sql        = slurp_file($path);
    my @statements = grep { /\S/ } map { trim($_) } split_sql_statements($sql);

    print "Applying $filename...\n";

    eval {
        $dbh->begin_work;
        for my $statement (@statements) {
            $dbh->do($statement);
        }
        $dbh->do( 'INSERT INTO schema_migrations (filename) VALUES (?)',
            undef, $filename );
        $dbh->commit;
        1;
    } or do {
        my $error = $@ || 'Unknown migration error';
        eval { $dbh->rollback };
        die "Failed while applying $filename: $error\n";
    };
}

print "Applied " . scalar(@pending) . " migration(s) to $db_path\n";

sub slurp_file {
    my ($path) = @_;

    open my $fh, '<', $path or die "Unable to read $path: $!\n";
    local $/;
    my $contents = <$fh>;
    close $fh;

    return $contents;
}

sub trim {
    my ($value) = @_;

    $value =~ s/^\s+//;
    $value =~ s/\s+\z//;

    return $value;
}

sub split_sql_statements {
    my ($sql) = @_;

    my @statements;
    my $buffer = q{};
    my $length = length $sql;

    my $in_single_quote  = 0;
    my $in_double_quote  = 0;
    my $in_line_comment  = 0;
    my $in_block_comment = 0;

    for ( my $index = 0 ; $index < $length ; $index++ ) {
        my $char = substr( $sql, $index, 1 );
        my $next = $index + 1 < $length ? substr( $sql, $index + 1, 1 ) : q{};

        if ($in_line_comment) {
            $buffer .= $char;
            if ( $char eq "\n" ) {
                $in_line_comment = 0;
            }
            next;
        }

        if ($in_block_comment) {
            $buffer .= $char;
            if ( $char eq '*' && $next eq '/' ) {
                $buffer .= $next;
                $index++;
                $in_block_comment = 0;
            }
            next;
        }

        if ( !$in_single_quote && !$in_double_quote ) {
            if ( $char eq '-' && $next eq '-' ) {
                $buffer .= $char . $next;
                $index++;
                $in_line_comment = 1;
                next;
            }

            if ( $char eq '/' && $next eq '*' ) {
                $buffer .= $char . $next;
                $index++;
                $in_block_comment = 1;
                next;
            }
        }

        if ( $char eq q{'} && !$in_double_quote ) {
            if ($in_single_quote) {
                if ( $next eq q{'} ) {
                    $buffer .= $char . $next;
                    $index++;
                    next;
                }
                $in_single_quote = 0;
            }
            else {
                $in_single_quote = 1;
            }
            $buffer .= $char;
            next;
        }

        if ( $char eq q{"} && !$in_single_quote ) {
            if ($in_double_quote) {
                if ( $next eq q{"} ) {
                    $buffer .= $char . $next;
                    $index++;
                    next;
                }
                $in_double_quote = 0;
            }
            else {
                $in_double_quote = 1;
            }
            $buffer .= $char;
            next;
        }

        if ( $char eq ';' && !$in_single_quote && !$in_double_quote ) {
            push @statements, $buffer;
            $buffer = q{};
            next;
        }

        $buffer .= $char;
    }

    push @statements, $buffer if $buffer =~ /\S/;

    return @statements;
}
