package Fork::Service::Forum;

use Moo;

use Fork::Crypt qw( generate_key_pair );

with 'Fork::Service';

sub get_forum {
    my ( $self, $forum_id ) = @_;
    my $sth = $self->dbh->prepare('SELECT * FROM forums WHERE id = ?');
    $sth->execute($forum_id);
    return $sth->fetchrow_hashref || undef;
}

sub list_forums {
    my ($self) = @_;
    my $sth = $self->dbh->prepare(
        'SELECT id, created_at FROM forums ORDER BY created_at DESC, id ASC');
    $sth->execute();
    return $sth->fetchall_arrayref( {} );
}

sub list_messages_for_forum {
    my ( $self, $forum_id ) = @_;
    my $sth = $self->dbh->prepare(
        q{
            SELECT messages.id,
                   messages.content,
                   messages.created_at,
                   messages.peer_id
              FROM messages
             WHERE messages.forum_id = ?
             ORDER BY messages.created_at DESC, messages.id DESC
        }
    );
    $sth->execute($forum_id);
    return $sth->fetchall_arrayref( {} );
}

sub join_forum {
    my ( $self,        $forum_id )   = @_;
    my ( $private_key, $public_key ) = generate_key_pair();
    my $sth = $self->dbh->prepare(
        'INSERT INTO forums ( id, public_key, private_key ) VALUES ( ?, ?, ? )'
    );
    $sth->execute( $forum_id, $public_key, $private_key );
    return $forum_id;
}

1;
