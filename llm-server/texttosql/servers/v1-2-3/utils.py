import sqlglot
from sqlglot import parse_one
from sqlglot.errors import ParseError
from typing import Tuple, Optional
import logging
import re

logger = logging.getLogger(__name__)

def validate_sql(sql: str) -> Tuple[bool, Optional[str]]:
    """Validate SQL syntax using sqlglot"""
    try:
        parse_one(sql, dialect='sqlite')
        return True, None
    except ParseError as e:
        return False, f"Parse error: {str(e)}"
    except Exception as e:
        return False, f"Validation error: {str(e)}"

def normalize_sql(sql: str) -> str:
    """Normalize SQL query - matches your training preprocessing"""
    sql = re.sub(r'\s+', ' ', sql.strip())
    return sql.rstrip(';')