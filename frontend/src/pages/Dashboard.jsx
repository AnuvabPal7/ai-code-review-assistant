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
  const navigate = useNavigate();
  const userName = localStorage.getItem('userName');

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
      fetchProjects();
    } catch (err) {
      setError('Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleReview = async (projectId) => {
    setReviewingId(projectId);
    setError('');
    try {
      const res = await api.post(`/reviews/${projectId}`);
      navigate(`/review/${res.data.reviewId}`);
    } catch (err) {
      setError('Review failed');
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
      <h2>Welcome, {userName}</h2>
      <button onClick={handleLogout}>Logout</button>
      <Link to="/history" style={{ marginLeft: '10px' }}>View Review History</Link>

      <h3>Upload a Java file for review</h3>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <form onSubmit={handleUpload}>
        <input type="file" accept=".java" onChange={(e) => setFile(e.target.files[0])} required />
        <button type="submit" disabled={uploading}>
          {uploading ? 'Uploading...' : 'Upload'}
        </button>
      </form>

      <h3>Your Projects</h3>
      {projects.length === 0 && <p>No projects uploaded yet.</p>}
      <ul>
        {projects.map((p) => (
          <li key={p.id}>
            {p.projectName} ({p.uploadType}) - {new Date(p.createdAt).toLocaleString()}
            <button onClick={() => handleReview(p.id)} disabled={reviewingId === p.id} style={{ marginLeft: '10px' }}>
              {reviewingId === p.id ? 'Reviewing...' : 'Run Review'}
            </button>
            <button
              onClick={() => handleDelete(p.id)}
              disabled={deletingId === p.id}
              style={{ marginLeft: '10px', color: 'red' }}
            >
              {deletingId === p.id ? 'Deleting...' : 'Delete'}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default Dashboard;
