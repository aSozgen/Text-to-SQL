package com.texttosql.backend.dto.search;

import com.texttosql.backend.dto.entity.ColumnDto;
import com.texttosql.backend.dto.entity.DatabaseDto;
import com.texttosql.backend.dto.entity.TableDto;
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