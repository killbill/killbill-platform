/*! SET storage_engine=INNODB */;

DROP TABLE IF EXISTS test_bundle;
CREATE TABLE test_bundle (
    record_id serial unique,
    is_started bool DEFAULT false,
    is_logged bool DEFAULT false,
    external_key varchar(128) NULL
    PRIMARY KEY(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
