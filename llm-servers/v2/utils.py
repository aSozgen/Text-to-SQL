import sqlglot
from sqlglot import parse_one
from sqlglot.errors import ParseError
from typing import Tuple, Optional
import logging
import re

logger = logging.getLogger(__name__)

def validate_sql(sql: str) -> Tuple[bool, Optional[str]]:
    """
    Validate SQL syntax using sqlglot
    
    Args:
        sql: SQL query string
        
    Returns:
        Tuple of (is_valid, error_message)
    """
    try:
        parsed = parse_one(sql, dialect='sqlite')
        return True, None
    except ParseError as e:
        return False, str(e)
    except Exception as e:
        return False, str(e)

def normalize_sql(sql: str) -> str:
    """Normalize SQL query for consistency"""
    sql = re.sub(r'\s+', ' ', sql.strip())
    return sql.rstrip(';')