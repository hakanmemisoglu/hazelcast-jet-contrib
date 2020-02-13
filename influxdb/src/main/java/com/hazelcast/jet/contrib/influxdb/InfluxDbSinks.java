/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.contrib.influxdb;

import com.hazelcast.function.SupplierEx;
import com.hazelcast.jet.impl.util.ExceptionUtil;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;

import static org.influxdb.BatchOptions.DEFAULTS;

/**
 * Contains factory methods for InfluxDB sinks
 */
public final class InfluxDbSinks {

    private InfluxDbSinks() {

    }

    /**
     * Creates a sink which pushes {@link Point} objects into the specified influxDB database
     *
     * @param name               Name of the created sink
     * @param connectionSupplier InfluxDB connection supplier. Batch must be enabled.
     */
    public static Sink<Point> influxDb(String name, SupplierEx<InfluxDB> connectionSupplier) {
        return SinkBuilder.sinkBuilder("influxDb-" + name,
                ctx -> connectionSupplier.get()
        ).<Point>receiveFn(InfluxDB::write)
                .flushFn(InfluxDB::flush)
                .destroyFn(InfluxDB::close)
                .build();
    }

    /**
     * Creates a sink which pushes {@link Point} objects into the specified influxDB database
     */
    public static Sink<Point> influxDb(String url, String database, String username, String password) {
        return influxDb("influxdb-" + database, () ->
                InfluxDBFactory.connect(url, username, password)
                               .setDatabase(database)
                               .enableBatch(
                                       DEFAULTS.exceptionHandler((points, throwable) -> ExceptionUtil.rethrow(throwable))
                               )
        );
    }
}
