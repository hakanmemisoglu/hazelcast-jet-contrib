/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.jet.contrib.imapjournal;

import com.hazelcast.config.Config;
import com.hazelcast.config.EventJournalConfig;
import com.hazelcast.map.IMap;
import com.hazelcast.jet.sql.SqlTestSupport;
import com.hazelcast.sql.SqlService;
import com.hazelcast.test.annotation.ParallelJVMTest;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Category({QuickTest.class, ParallelJVMTest.class})
public class IMapJournalConnectorTest extends SqlTestSupport {

    private static SqlService sqlService;

    @BeforeClass
    public static void setUpClass() {
        EventJournalConfig journalConfig = new EventJournalConfig()
                .setEnabled(true)
                .setCapacity(100000)
                .setTimeToLiveSeconds(100);
        Config config = new Config();
        config.getJetConfig().setEnabled(true);
        config.getMapConfig("map1").setEventJournalConfig(journalConfig);
        config.getMapConfig("map2").setEventJournalConfig(journalConfig);
        config.getMapConfig("map3").setEventJournalConfig(journalConfig);
        config.getMapConfig("map4").setEventJournalConfig(journalConfig);

        initialize(2, config);

        sqlService = instance().getSql();
    }

    @Test
    public void test() {
        String mapName = "map1";
        IMap<Integer, String> map = instance().getMap(mapName);

        sqlService.execute("CREATE MAPPING " + " " + mapName + " "
                + "TYPE IMAP_JOURNAL "
                + "OPTIONS ("
                + "'keyFormat'='int', "
                + "'valueFormat'='varchar'"
                + ")");

        List<Row> expectedRows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            map.set(i, "Value " + i);
            expectedRows.add(new Row(i, null, "Value " + i, "ADDED"));
        }
        for (int i = 0; i < 10; i++) {
            map.set(i, "Updated Value " + i);
            expectedRows.add(new Row(i, "Value " + i, "Updated Value " + i, "UPDATED"));
        }
        for (int i = 0; i < 10; i++) {
            map.delete(i);
            expectedRows.add(new Row(i, "Updated Value " + i, null, "REMOVED"));
        }

        assertRowsEventuallyInAnyOrder("SELECT __key, \"old\", \"new\", type FROM " + mapName, expectedRows);
    }

    @Test
    public void test2() {
        String mapName = "map2";
        IMap<String, Integer> map = instance().getMap(mapName);

        sqlService.execute("CREATE MAPPING " + " " + mapName + " "
                + "TYPE IMAP_JOURNAL "
                + "OPTIONS ("
                + "'keyFormat'='varchar', "
                + "'valueFormat'='int'"
                + ")");

        List<Row> expectedRows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            map.set("Key " + i, i);
            expectedRows.add(new Row("Key " + i, null, i, "ADDED"));
        }
        for (int i = 0; i < 10; i++) {
            map.set("Key " + i, i + 1);
            expectedRows.add(new Row("Key " + i, i, i + 1, "UPDATED"));
        }
        for (int i = 0; i < 10; i++) {
            map.delete("Key " + i);
            expectedRows.add(new Row("Key " + i, i + 1, null, "REMOVED"));
        }

        assertRowsEventuallyInAnyOrder("SELECT __key, \"old\", \"new\", type FROM " + mapName, expectedRows);

    }

    @Test
    public void test3() {
        String mapName = "map3";
        IMap<String, Integer> map = instance().getMap(mapName);

        sqlService.execute("CREATE MAPPING " + " " + mapName + " "
                + "TYPE IMAP_JOURNAL "
                + "OPTIONS ("
                + "'keyFormat'='varchar', "
                + "'valueFormat'='int'"
                + ")");

        List<Row> expectedRows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            map.set("Key " + i, i);
            expectedRows.add(new Row("Key " + i, null, i, "ADDED"));
        }
        for (int i = 0; i < 10; i++) {
            map.set("Key " + i, i + 1);
            expectedRows.add(new Row("Key " + i, i, i + 1, "UPDATED"));
        }
        for (int i = 0; i < 10; i++) {
            map.delete("Key " + i);
            expectedRows.add(new Row("Key " + i, i + 1, null, "REMOVED"));
        }

        assertRowsEventuallyInAnyOrder("SELECT __key, \"old\", \"new\", type FROM " + mapName, expectedRows);

    }

    @Test
    public void test4() {
        String mapName = "map4";
        IMap<String, Person> map = instance().getMap(mapName);

        sqlService.execute("CREATE MAPPING " + " " + mapName + " "
                + "TYPE IMAP_JOURNAL "
                + "OPTIONS ("
                + "'keyFormat'='java', "
                + "'keyJavaClass'='" + String.class.getName() + "', "
                + "'valueFormat'='java', "
                + "'valueJavaClass'='" + Person.class.getName() + "'"
                + ")");

        List<Row> expectedRows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            map.set("Name " + i, new Person("Name " + i, i));
            expectedRows.add(new Row("Name " + i, null, i, "ADDED"));
        }
        for (int i = 0; i < 10; i++) {
            map.set("Name " + i, new Person("Name " + i, i + 1));
            expectedRows.add(new Row("Name " + i, i, i + 1, "UPDATED"));
        }
        for (int i = 0; i < 10; i++) {
            map.delete("Name " + i);
            expectedRows.add(new Row("Name " + i, i + 1, null, "REMOVED"));
        }

        assertRowsEventuallyInAnyOrder("SELECT __key, old_age + 100, new_age * 100, type FROM " + mapName, expectedRows);

    }

    public static class Person implements Serializable {
        public String name;
        public Integer age;

        public Person(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
}