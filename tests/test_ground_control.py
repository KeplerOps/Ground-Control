import os
import json
import pytest
from unittest.mock import Mock
from ground_control.main import (
    sanitize_filename,
    get_type_prefix,
    check_directory,
)

def test_sanitize_filename():
    """Test filename sanitization."""
    # Test invalid characters
    assert sanitize_filename('test<>:"/\\|?*file') == "test_________file"
    
    # Test leading/trailing spaces and dots
    assert sanitize_filename(" test.file. ") == "test.file"
    
    # Test mixed case
    assert sanitize_filename("TEST file") == "TEST file"
    
    # Test empty string
    assert sanitize_filename("") == ""

def test_get_type_prefix():
    """Test issue type prefix generation."""
    # Test various issue types
    assert get_type_prefix("Initiative") == "INI"
    assert get_type_prefix("Technical Initiative") == "INI"
    assert get_type_prefix("Epic") == "EPIC"
    assert get_type_prefix("Story") == "STORY"
    assert get_type_prefix("User Story") == "STORY"
    assert get_type_prefix("Task") == "TASK"
    assert get_type_prefix("Technical Task") == "TASK"
    
    # Test case insensitivity
    assert get_type_prefix("INITIATIVE") == "INI"
    assert get_type_prefix("epic") == "EPIC"
    assert get_type_prefix("Story") == "STORY"
    
    # Test unknown types default to TASK
    assert get_type_prefix("Unknown") == "TASK"
    assert get_type_prefix("Custom Type") == "TASK"

def test_check_directory(tmp_path):
    """Test directory checking and creation."""
    # Test creating new directory
    new_dir = tmp_path / "new_dir"
    check_directory(str(new_dir))
    assert new_dir.exists()
    assert new_dir.is_dir()
    
    # Test empty existing directory
    empty_dir = tmp_path / "empty_dir"
    empty_dir.mkdir()
    check_directory(str(empty_dir))  # Should not raise
    
    # Test non-empty directory
    nonempty_dir = tmp_path / "nonempty_dir"
    nonempty_dir.mkdir()
    (nonempty_dir / "file.txt").write_text("content")
    with pytest.raises(ValueError) as exc:
        check_directory(str(nonempty_dir))
    assert "not empty" in str(exc.value)
    
    # Test file path
    file_path = tmp_path / "file.txt"
    file_path.write_text("content")
    with pytest.raises(ValueError) as exc:
        check_directory(str(file_path))
    assert "not a directory" in str(exc.value) 