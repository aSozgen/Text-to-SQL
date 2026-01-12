package com.texttosql.backend.dto.search;

import com.texttosql.backend.dto.ColumnDto;
import com.texttosql.backend.dto.DatabaseDto;
import com.texttosql.backend.dto.TableDto;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SchemaSearchResponse {
    private List<DatabaseDto> databases;
    private List<TableDto> tables;
    private List<ColumnDto> columns;
}