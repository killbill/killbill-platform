/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/* We cannot use timestamp in MySQL because of the implicit TimeZone conversions it does behind the scenes */
DROP DOMAIN IF EXISTS datetime CASCADE;
CREATE DOMAIN datetime AS timestamp without time zone;
/* TEXT in MySQL is smaller then MEDIUMTEXT */
DROP DOMAIN IF EXISTS mediumtext CASCADE;
CREATE DOMAIN mediumtext AS text;
/* PostgreSQL uses BYTEA to manage all BLOB types */
DROP DOMAIN IF EXISTS mediumblob CASCADE;
CREATE DOMAIN mediumblob AS bytea;

CREATE OR REPLACE LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION last_insert_id() RETURNS BIGINT AS $$
    DECLARE
        result BIGINT;
    BEGIN
        SELECT lastval() INTO result;
        RETURN result;
    EXCEPTION WHEN OTHERS THEN
        SELECT NULL INTO result;
        RETURN result;
    END;
$$ LANGUAGE plpgsql VOLATILE;

CREATE OR REPLACE FUNCTION schema() RETURNS VARCHAR AS $$
    DECLARE
        result VARCHAR;
    BEGIN
        SELECT current_schema() INTO result;
        RETURN result;
    EXCEPTION WHEN OTHERS THEN
        SELECT NULL INTO result;
        RETURN result;
    END;
$$ LANGUAGE plpgsql IMMUTABLE;
