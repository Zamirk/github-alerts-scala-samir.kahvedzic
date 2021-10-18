DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS repositories;

create table users(
    user_id             UUID        NOT NULL,
    slack_user_id       VARCHAR(20) NOT NULL,
    slack_channel_id    VARCHAR(20) NOT NULL,
    primary key (user_id)
);

create table repositories(
    repository_id       UUID            NOT NULL,
    owner               VARCHAR(20)     NOT NULL,
    repository          VARCHAR(20)     NOT NULL,
    primary key (repository_id)
);

create table subscriptions(
    user_id             UUID            NOT NULL,
    repository_id       UUID            NOT NULL,
    subscribed_at       VARCHAR(20)     NOT NULL,
    subscription_type   VARCHAR(20)     NOT NULL,
    primary key (user_id, repository_id),
    CONSTRAINT fk_user_id
        FOREIGN KEY(user_id)
            REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_repository_id
        FOREIGN KEY(repository_id)
            REFERENCES repositories(repository_id)
);