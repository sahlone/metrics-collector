#!/bin/bash

psql -v ON_ERROR_STOP=1 << --eosql--
create database metrics_collector ;
create user metrics_collector with encrypted password 'metricscollector';
grant all privileges on database metrics_collector to metrics_collector;
--eosql--

psql -v ON_ERROR_STOP=1 metrics_collector << --eosql--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--eosql--
