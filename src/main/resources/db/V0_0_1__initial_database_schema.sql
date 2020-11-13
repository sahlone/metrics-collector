CREATE TABLE sensor_data
(
    sd_id           UUID PRIMARY KEY,
    sd_sensor_id    UUID                     not null,
    sd_metric_name  TEXT                     not null,
    sd_metric_value NUMERIC                  not null,
    sd_timestamp    TIMESTAMP WITH TIME ZONE NOT NULL,
    _created        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp()
);

create index idx_sd_sensorId on sensor_data (sd_sensor_id);
create index idx_sd_timestamp on sensor_data (sd_timestamp DESC);

CREATE TABLE sensor_status
(
    ss_id        UUID PRIMARY KEY,
    ss_sensor_id UUID UNIQUE              NOT NULL,
    ss_state     TEXT                     NOT NULL,
    ss_created   TIMESTAMP WITH TIME ZONE NOT NULL,
    ss_modified  TIMESTAMP WITH TIME ZONE NOT NULL,
    _created     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE sensor_alarms
(
    sa_id        UUID PRIMARY KEY,
    sa_sensor_id UUID                     NOT NULL,
    sa_start     TIMESTAMP WITH TIME ZONE NOT NULL,
    sa_end       TIMESTAMP WITH TIME ZONE NULL,
    sa_state     TEXT                     NOT NULL,
    _created     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp()
);
create index idx_sa_sensorId on sensor_alarms (sa_sensor_id);

CREATE TABLE sensor_metrics
(
    sm_id        UUID PRIMARY KEY,
    sm_sensor_id UUID UNIQUE              NOT NULL,
    sm_avg       NUMERIC                  NOT NULL,
    sm_max       NUMERIC                  NOT NULL,
    sm_count     NUMERIC                  NOT NULL,
    sm_created   TIMESTAMP WITH TIME ZONE NOT NULL,
    sm_modified  TIMESTAMP WITH TIME ZONE NOT NULL,
    _created     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp()
);
