/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foundationdb.server.entity.changes;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.foundationdb.ais.model.AbstractVisitor;
import com.foundationdb.ais.model.aisb2.NewTableBuilder;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foundationdb.ais.model.Table;
import com.foundationdb.ais.model.TableName;
import com.foundationdb.ais.model.aisb2.AISBBasedBuilder;
import com.foundationdb.ais.model.aisb2.NewAISBuilder;
import com.foundationdb.server.api.DDLFunctions;
import com.foundationdb.server.service.session.Session;

public final class EntityParser {
    public static final String PK_COL_NAME = "_id";

    public static String parentRefColName(String parentName) {
        return "_" + parentName + PK_COL_NAME;
    }


    private static final Logger LOG = LoggerFactory.getLogger(EntityParser.class);
    private final boolean alwaysWithPK;
    private int stringWidth = 128;

    public EntityParser() {
        this(false);
    }

    public EntityParser(boolean alwaysWithPK) {
        this.alwaysWithPK = alwaysWithPK;
    }
        
    public void setStringWidth(int width) {
        stringWidth = width;
    }
    public Table parse (final DDLFunctions ddlFunctions, final Session session,
                        TableName tableName, JsonNode node) throws IOException {
        NewAISBuilder builder = AISBBasedBuilder.create(tableName.getSchemaName(),
                                                        ddlFunctions.getTypesRegistry());
        processContainer (node, builder, tableName);
        return builder.ais().getTable(tableName);
    }

    public Table create (final DDLFunctions ddlFunctions, final Session session, Table newRoot) throws IOException {
        newRoot.visit(new AbstractVisitor() {
            @Override
            public void visit(Table table) {
                ddlFunctions.createTable(session, table);
            }
        });
        return ddlFunctions.getTable(session, newRoot.getName());
    }

    public Table parseAndCreate (final DDLFunctions ddlFunctions, final Session session,
                                     TableName tableName, JsonNode node) throws IOException {
        return create(ddlFunctions, session, parse(ddlFunctions, session,
                                                   tableName, node));
    }
    
    private void processContainer (JsonNode node, NewAISBuilder builder, TableName tableName) throws IOException {
        boolean first = true;
        if (node.isObject()) {
            processTable (node, builder, tableName);
        } else if (node.isArray()) {
            // For an array of elements, process the first one and discard the rest
            for (JsonNode arrayElement : node) {
                if (first && arrayElement.isObject()) { 
                    processTable(arrayElement, builder, tableName);
                    first = false;
                }
                else if (first && !arrayElement.isContainerNode()) {
                    NewTableBuilder table = builder.table(tableName.getSchemaName(), tableName.getTableName());
                    addColumnToTable (arrayElement, "value", table);
                    first = false;
                }
                // else throw Bad Json Format Exception
            }
            // If no elements in the array, add a placeholder column
            if (first) {
                NewTableBuilder table = builder.table(tableName.getSchemaName(), tableName.getTableName());
                table.colString("placeholder", stringWidth, true);
            }
        }
        // else throw Bad Json Format Exception
    }

    private void processTable (JsonNode node, NewAISBuilder builder, TableName tableName) throws IOException {
        
        LOG.trace("Creating Table {}", tableName);
        // Pass one, insert fields from the table
        boolean columnsAdded = false;
        NewTableBuilder table = builder.table(tableName.getSchemaName(), tableName.getTableName());

        if(alwaysWithPK) {
            addPK(table);
            columnsAdded = true;
        }

        Iterator<Entry<String,JsonNode>> i = node.fields();
        while (i.hasNext()) {
            Entry<String,JsonNode> field = i.next();
            if (field.getValue().isValueNode()) {
                LOG.trace("Column {}", field.getKey());
                addColumnToTable(field.getValue(), field.getKey(), table);
                columnsAdded = true;
            }
        }
        
        if (!columnsAdded) {
            table.colString("placeholder", stringWidth, true);
            LOG.trace("Column added placeholder");
        }
        // pass 2: insert the child nodes
        boolean first = true;
        i = node.fields();
        while (i.hasNext()) {
            Entry<String,JsonNode> field = i.next();
            if (field.getValue().isContainerNode()) {
                LOG.trace("Creating child table {} - first {}", field.getKey(), first);
                if (first && !alwaysWithPK) {
                    addPK(table);
                }
                first = false;
                TableName childTable = TableName.parse(tableName.getSchemaName(), field.getKey());
                processContainer (field.getValue(), builder, childTable);
                NewTableBuilder child = builder.getTable(childTable);
                String parentRefName = parentRefColName(tableName.getTableName());
                child.colLong(parentRefName);
                LOG.trace("Column added {}", parentRefName);
                child.joinTo(tableName).on(parentRefName, PK_COL_NAME);
                builder.getTable(tableName);
            }
        }
    }
    private static final Pattern DATE_PATTERN = Pattern.compile("^((\\d+)-(\\d+)-(\\d+)).*");    
    private void addColumnToTable (JsonNode node, String name, NewTableBuilder table) {
        if (node.isTextual()) {
            boolean dateColumn = false;
            // Do a simple "could be a date" check first, if so, do an exact verify.
            Matcher m = DATE_PATTERN.matcher(node.asText().trim());
            if (m.matches()) {
                try {
                    ISODateTimeFormat.dateTimeParser().parseDateTime(node.asText());
                    table.colDateTime(name, true);
                    dateColumn = true;
                } catch (IllegalArgumentException ex) {
                    dateColumn = false;
                }
            }
            if (!dateColumn) {
                int  len = Math.max(node.asText().length(), stringWidth);
                table.colString(name, len, true);
            }
        } else if (node.isIntegralNumber()) {
            table.colBigInt(name, true);
        } else if (node.isDouble()) {
            table.colDouble(name, true);
        } else if (node.isBoolean()) {
            table.colBoolean(name, true);
        } else if (node.isNull()) {
            // wild guess
            table.colString(name, stringWidth, true);
        }
    }

    private void addPK(NewTableBuilder builder) {
        builder.autoIncLong(PK_COL_NAME, 1);
        builder.pk(PK_COL_NAME);
    }
}
