package Fork::HTTP::Handler::Forum;

use Moo;

use Fork::Service::Forum;

with 'Fork::HTTP::Handler';

has forum_service => (
    is      => 'lazy',
    default => sub {
        Fork::Service::Forum->new( dbh => shift->dbh );
    }
);

sub index {
    my ($self) = @_;

    return $self->template(
        'index',
        {
            forums => $self->forum_service->list_forums,
        }
    );
}

sub join_forum {
    my ( $self, $args ) = @_;

    my $forum = $self->req->body_parameters->get('forum') || $args->{forum};

    if ( !$forum ) {
        return $self->error( 400, 'Missing forum parameter' );
    }

    if ( $self->forum_service->get_forum($forum) ) {
        return $self->redirect("/f/$forum");
    }

    my $forum_id = $self->forum_service->join_forum($forum);

    return $self->redirect("/f/$forum_id");
}

sub forum {
    my ( $self, $args ) = @_;

    my $forum_id = $args->{forum};

    if ( !$forum_id ) {
        return $self->error( 400, 'Missing forum parameter' );
    }

    my $forum = $self->forum_service->get_forum($forum_id);

    if ( !$forum ) {
        return $self->error( 404, 'Not Found' );
    }

    return $self->template(
        'forum',
        {
            forum    => $forum,
            messages => $self->forum_service->list_messages_for_forum($forum_id),
        }
    );
}

1;
