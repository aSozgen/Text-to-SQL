package com.texttosql.backend.service;

import com.texttosql.backend.repository.ColumnRepository;
import com.texttosql.backend.repository.DatabaseRepository;
import com.texttosql.backend.repository.TableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SchemaService {

    private final DatabaseRepository databaseRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;

    public SchemaService(DatabaseRepository databaseRepository, TableRepository tableRepository, ColumnRepository columnRepository) {
        this.databaseRepository = databaseRepository;
        this.tableRepository = tableRepository;
        this.columnRepository = columnRepository;
    }
}
