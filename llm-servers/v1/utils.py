import sqlglot
from sqlglot import parse_one
from sqlglot.errors import ParseError
from typing import Tuple, Optional
import logging
import re

logger = logging.getLogger(__name__)


def normalize_sql(sql: str) -> str:
    """Normalize SQL query"""
    # Remove extra whitespace
    sql = re.sub(r'\s+', ' ', sql.strip())
    
    # Remove trailing semicolon
    sql = sql.rstrip(';')
    
    return sql


def validate_sql(sql: str) -> Tuple[bool, Optional[str]]:
    """
    Validate SQL syntax using sqlglot
    
    Args:
        sql: SQL query string
        
    Returns:
        Tuple of (is_valid, error_message)
    """
    try:
        # Normalize first
        sql = normalize_sql(sql)
        
        # Parse and validate
        parsed = parse_one(sql, dialect='sqlite')
        return True, None
        
    except ParseError as e:
        error_msg = str(e)
        logger.debug(f"SQL validation failed: {error_msg}")
        return False, error_msg
        
    except Exception as e:
        error_msg = str(e)
        logger.warning(f"Unexpected validation error: {error_msg}")
        return False, error_msg


def sanitize_input(text: str, max_length: int = 500) -> str:
    """Sanitize user input"""
    # Remove null bytes
    text = text.replace('\x00', '')
    
    # Trim whitespace
    text = text.strip()
    
    # Truncate if too long
    if len(text) > max_length:
        text = text[:max_length]
        logger.warning(f"Input truncated to {max_length} chars")
    
    return text