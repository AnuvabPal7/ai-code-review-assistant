import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';

function Dashboard() {
  const [projects, setProjects] = useState([]);
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [reviewingId, setReviewingId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const navigate = useNavigate();
  const userName = localStorage.getItem('userName');

  const [showPasteForm, setShowPasteForm] = useState(false);
  const [snippetFileName, setSnippetFileName] = useState('Snippet.java');
  const [snippetCode, setSnippetCode] = useState('');
  const [submittingSnippet, setSubmittingSnippet] = useState(false);

  const fetchProjects = async () => {
    try {
      const res = await api.get('/projects');
      setProjects(res.data);
    } catch (err) {
      setError('Failed to load projects');
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(''), 4000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) return;
    setUploading(true);
    setError('');
    try {
      const formData = new FormData();
      formData.append('file', file);
      await api.post('/projects/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setFile(null);
      setSuccessMessage('File uploaded successfully.');
      fetchProjects();
    } catch (err) {
      setError('Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmitSnippet = async (e) => {
    e.preventDefault();
    if (!snippetCode.trim()) return;
    setSubmittingSnippet(true);
    setError('');
    try {
      await api.post('/projects/submit-code', {
        fileName: snippetFileName || 'Snippet.java',
        code: snippetCode,
      });
      setSnippetCode('');
      setSnippetFileName('Snippet.java');
      setShowPasteForm(false);
      setSuccessMessage('Code snippet submitted successfully.');
      fetchProjects();
    } catch (err) {
      setError('Snippet submission failed');
    } finally {
      setSubmittingSnippet(false);
    }
  };

  const handleReview = async (projectId) => {
    setReviewingId(projectId);
    setError('');
    try {
      const res = await api.post(`/reviews/${projectId}`);
      navigate(`/review/${res.data.reviewId}`);
    } catch (err) {
      const message = err.response?.data?.message || 'Review failed';
      setError(message);
    } finally {
      setReviewingId(null);
    }
  };

  const handleDelete = async (projectId) => {
    if (!window.confirm('Delete this project and all its review history?')) return;
    setDeletingId(projectId);
    setError('');
    try {
      await api.delete(`/projects/${projectId}`);
      setSuccessMessage('Project deleted.');
      fetchProjects();
    } catch (err) {
      setError('Delete failed');
    } finally {
      setDeletingId(null);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userName');
    navigate('/login');
  };

  return (
    <div>
      <div className="nav-bar">
        <h2>Welcome, {userName}</h2>
        <Link to="/history">Review History</Link>
        <Link to="/profile">Profile Settings</Link>
        <button className="secondary" onClick={handleLogout}>Logout</button>
      </div>

      {successMessage && <div className="message success">{successMessage}</div>}
      {error && <div className="message error">{error}</div>}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Upload a Java file for review</h3>
        <form className="inline" onSubmit={handleUpload}>
          <input type="file" accept=".java" onChange={(e) => setFile(e.target.files[0])} required />
          <button type="submit" disabled={uploading}>
            {uploading ? 'Uploading...' : 'Upload'}
          </button>
        </form>

        <div style={{ marginTop: '16px' }}>
          <button className="secondary" onClick={() => setShowPasteForm(!showPasteForm)}>
            {showPasteForm ? 'Hide paste form' : 'Or paste code directly'}
          </button>
        </div>

        {showPasteForm && (
          <form onSubmit={handleSubmitSnippet} style={{ marginTop: '14px' }}>
            <div>
              <input
                type="text"
                value={snippetFileName}
                onChange={(e) => setSnippetFileName(e.target.value)}
                placeholder="Snippet.java"
                style={{ maxWidth: '220px' }}
              />
              <span className="muted-hint" style={{ marginLeft: '8px' }}>
                Only .java files are currently supported
              </span>
            </div>
            <textarea
              value={snippetCode}
              onChange={(e) => setSnippetCode(e.target.value)}
              placeholder="Paste your Java code here..."
              rows="12"
              required
            />
            <button type="submit" disabled={submittingSnippet} style={{ maxWidth: '200px' }}>
              {submittingSnippet ? 'Submitting...' : 'Submit for Review'}
            </button>
          </form>
        )}
      </div>

      <h3>Your Projects</h3>
      {projects.length === 0 && <p className="muted-hint">No projects uploaded yet.</p>}
      <ul className="project-list">
        {projects.map((p) => (
          <li key={p.id} className="project-item">
            <div>
              <div className="project-info">
                {p.projectName}
                <span className="tag">{p.uploadType}</span>
                <span className="tag">{p.detectedLanguage}</span>
                {!p.supported && <span className="tag unsupported">not supported</span>}
              </div>
              <div className="project-meta">{new Date(p.createdAt).toLocaleString()}</div>
            </div>
            <div className="action-group">
              <button onClick={() => handleReview(p.id)} disabled={reviewingId === p.id}>
                {reviewingId === p.id ? 'Reviewing...' : 'Run Review'}
              </button>
              <button
                className="danger"
                onClick={() => handleDelete(p.id)}
                disabled={deletingId === p.id}
              >
                {deletingId === p.id ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default Dashboard;