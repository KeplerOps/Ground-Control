import os
import json
import pytest
from unittest.mock import Mock, patch
from ground_control.main import (
    sanitize_filename,
    get_type_prefix,
    check_directory,
    get_issue_relationships,
    create_ticket_directory,
    parse_args,
    cleanup_directory,
    main,
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

def test_get_issue_relationships():
    """Test relationship detection with different issue types."""
    # Mock JIRA client
    mock_jira = Mock()
    
    # Mock issue with parent
    parent_issue = Mock()
    parent_issue.key = "SECOPS-100"
    parent_issue.fields.issuetype.name = "Epic"
    
    issue_with_parent = Mock()
    issue_with_parent.key = "SECOPS-101"
    issue_with_parent.fields.parent = parent_issue
    issue_with_parent.fields.customfield_10014 = None
    
    rels = get_issue_relationships(issue_with_parent, mock_jira)
    assert rels["parent"] == {
        "key": "SECOPS-100",
        "type": "Epic"
    }
    
    # Mock issue with epic link
    epic_issue = Mock()
    epic_issue.key = "SECOPS-200"
    epic_issue.fields.issuetype.name = "Epic"
    mock_jira.issue.return_value = epic_issue
    
    issue_with_epic = Mock()
    issue_with_epic.key = "SECOPS-201"
    issue_with_epic.fields.parent = None
    issue_with_epic.fields.customfield_10014 = "SECOPS-200"
    
    rels = get_issue_relationships(issue_with_epic, mock_jira)
    assert rels["parent"] == {
        "key": "SECOPS-200",
        "type": "Epic"
    }
    
    # Mock issue with no relationships
    issue_no_parent = Mock()
    issue_no_parent.key = "SECOPS-300"
    issue_no_parent.fields.parent = None
    issue_no_parent.fields.customfield_10014 = None
    
    rels = get_issue_relationships(issue_no_parent, mock_jira)
    assert rels["parent"] is None

def test_create_ticket_directory(tmp_path):
    """Test ticket directory creation and content."""
    # Mock JIRA issue
    issue = Mock()
    issue.key = "SECOPS-123"
    issue.id = "10000"
    issue.fields.issuetype.name = "Story"
    issue.fields.status.name = "In Progress"
    issue.fields.summary = "Test ticket"
    issue.fields.reporter = "John Doe"
    issue.fields.assignee = "Jane Smith"
    issue.fields.updated = "2024-01-22T12:34:56"
    issue.fields.description = "Test description"
    
    # Mock JIRA client
    jira = Mock()
    jira.comments.return_value = [
        Mock(
            author=Mock(displayName="Commenter"),
            updated="2024-01-22T13:00:00",
            body="Test comment"
        )
    ]
    
    # Create ticket directory
    with patch('ground_control.main.get_issue_relationships') as mock_get_rels:
        mock_get_rels.return_value = {"parent": None, "children": []}
        dir_path = create_ticket_directory(issue, jira, tmp_path)
    
    # Check directory structure
    assert os.path.exists(dir_path)
    assert os.path.exists(os.path.join(dir_path, "metadata.json"))
    assert os.path.exists(os.path.join(dir_path, "ticket.md"))
    
    # Check metadata content
    with open(os.path.join(dir_path, "metadata.json")) as f:
        metadata = json.load(f)
        assert metadata["key"] == "SECOPS-123"
        assert metadata["type"] == "Story"
        assert metadata["status"] == "In Progress"
    
    # Check markdown content
    with open(os.path.join(dir_path, "ticket.md")) as f:
        content = f.read()
        assert "# SECOPS-123: Test ticket" in content
        assert "Test description" in content
        assert "Test comment" in content

def test_parse_args():
    """Test command line argument parsing."""
    # Test default arguments
    with patch('sys.argv', ['ground-control']):
        args = parse_args()
        assert args.output == "tickets"
        assert args.ticket is None
        assert not args.recursive
    
    # Test output directory
    with patch('sys.argv', ['ground-control', '-o', 'custom_dir']):
        args = parse_args()
        assert args.output == "custom_dir"
    
    # Test specific ticket
    with patch('sys.argv', ['ground-control', 'SECOPS-123']):
        args = parse_args()
        assert args.ticket == "SECOPS-123"
    
    # Test recursive flag
    with patch('sys.argv', ['ground-control', 'SECOPS-123', '-r']):
        args = parse_args()
        assert args.ticket == "SECOPS-123"
        assert args.recursive 

def test_cleanup_directory(tmp_path):
    """Test directory cleanup functionality."""
    # Create test directory structure
    test_dir = tmp_path / "test_cleanup"
    test_dir.mkdir()
    
    # Create some files
    (test_dir / "file1.txt").write_text("content1")
    (test_dir / "file2.txt").write_text("content2")
    
    # Create a subdirectory with files
    subdir = test_dir / "subdir"
    subdir.mkdir()
    (subdir / "file3.txt").write_text("content3")
    
    # Run cleanup
    cleanup_directory(str(test_dir))
    
    # Verify directory is empty
    assert os.path.exists(test_dir)
    assert len(os.listdir(test_dir)) == 0

def test_create_ticket_directory_long_summary(tmp_path):
    """Test ticket directory creation with long summary."""
    # Mock JIRA issue with long summary
    issue = Mock()
    issue.key = "SECOPS-123"
    issue.id = "10000"
    issue.fields.issuetype.name = "Story"
    issue.fields.status.name = "In Progress"
    issue.fields.summary = "This is a very long summary that should be truncated because it exceeds fifty characters"
    issue.fields.reporter = "John Doe"
    issue.fields.assignee = None  # Test None assignee
    issue.fields.updated = "2024-01-22T12:34:56"
    issue.fields.description = "Test description"
    
    # Mock JIRA client
    jira = Mock()
    jira.comments.return_value = []  # Test no comments
    
    # Create ticket directory
    with patch('ground_control.main.get_issue_relationships') as mock_get_rels:
        mock_get_rels.return_value = {"parent": None, "children": []}
        dir_path = create_ticket_directory(issue, jira, tmp_path)
    
    # Verify directory name is truncated
    dir_name = os.path.basename(dir_path)
    assert len(dir_name) < 100  # Reasonable length
    assert dir_name.startswith("STORY-SECOPS-123-This is a very long summary that")
    assert dir_name.endswith("...")

@patch('ground_control.main.JIRA')
@patch('ground_control.main.USERNAME', 'test@example.com')
@patch('ground_control.main.API_TOKEN', 'test-token')
@patch('ground_control.main.JIRA_URL', 'https://jira.example.com')
@patch('ground_control.main.BOARD_ID', 'SECOPS')
def test_main_jira_connection(mock_jira_class, *_):
    """Test main function's JIRA connection and error handling."""
    # Mock environment variables
    env_vars = {
        "JIRA_URL": "https://jira.example.com",
        "JIRA_PROJECT": "SECOPS",
        "JIRA_USERNAME": "test@example.com",
        "JIRA_API_TOKEN": "test-token"
    }
    
    with patch.dict(os.environ, env_vars, clear=True), \
         patch('sys.argv', ['ground-control', '-o', 'test_output']), \
         patch('ground_control.main.check_directory'), \
         patch('ground_control.main.cleanup_directory'):
        
        # Mock JIRA client and search results
        mock_jira_instance = Mock()
        mock_jira_class.return_value = mock_jira_instance
        
        # Mock issues of different types
        initiative = Mock()
        initiative.fields = Mock()
        initiative.fields.issuetype = Mock()
        initiative.fields.issuetype.name = "Initiative"
        initiative.key = "SECOPS-1"
        
        epic = Mock()
        epic.fields = Mock()
        epic.fields.issuetype = Mock()
        epic.fields.issuetype.name = "Epic"
        epic.key = "SECOPS-2"
        
        story = Mock()
        story.fields = Mock()
        story.fields.issuetype = Mock()
        story.fields.issuetype.name = "Story"
        story.key = "SECOPS-3"
        
        # Set up mock search results
        mock_jira_instance.search_issues.return_value = [initiative, epic, story]
        
        # Run main
        with patch('ground_control.main.create_ticket_directory') as mock_create_dir:
            mock_create_dir.return_value = "test_dir"
            exit_code = main()
        
        # Verify JIRA client creation
        mock_jira_class.assert_called_once_with(
            server="https://jira.example.com",
            basic_auth=("test@example.com", "test-token")
        )
        
        # Verify issue search
        mock_jira_instance.search_issues.assert_called()
        
        # Verify exit code
        assert exit_code == 0

@patch('ground_control.main.JIRA')
def test_main_missing_credentials(mock_jira_class):
    """Test main function with missing credentials."""
    # Mock environment variables with missing credentials
    env_vars = {
        "JIRA_URL": "https://jira.example.com",
        "JIRA_PROJECT": "SECOPS",
        "JIRA_USERNAME": "<your-email>",  # Default value
        "JIRA_API_TOKEN": "<your-api-token>"  # Default value
    }
    
    with patch.dict(os.environ, env_vars), \
         patch('sys.argv', ['ground-control']):
        exit_code = main()
        
        # Should exit with error
        assert exit_code == 1
        # JIRA client should not be created
        mock_jira_class.assert_not_called() 