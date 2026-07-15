import { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

function ReviewHistory() {
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [deletingId, setDeletingId] = useState(null);

  const [searchTerm, setSearchTerm] = useState('');
  const [minScore, setMinScore] = useState('');
  const [maxScore, setMaxScore] = useState('');

  const fetchHistory = async () => {
    try {
      const res = await api.get('/reviews/history');
      setHistory(res.data);
    } catch (err) {
      setError('Failed to load history');
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  const handleDelete = async (reviewId) => {
    if (!window.confirm('Delete this review? This cannot be undone.')) return;
    setDeletingId(reviewId);
    setError('');
    try {
      await api.delete(`/reviews/${reviewId}`);
      setSuccessMessage('Review deleted.');
      fetchHistory();
    } catch (err) {
      setError('Failed to delete review');
    } finally {
      setDeletingId(null);
    }
  };

  const filteredHistory = useMemo(() => {
    return history.filter((h) => {
      const matchesSearch = h.projectName.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesMin = minScore === '' || h.score >= parseInt(minScore, 10);
      const matchesMax = maxScore === '' || h.score <= parseInt(maxScore, 10);
      return matchesSearch && matchesMin && matchesMax;
    });
  }, [history, searchTerm, minScore, maxScore]);

  return (
    <div>
      <div className="nav-bar">
        <h2>Review History</h2>
        <Link to="/dashboard">Back to Dashboard</Link>
      </div>

      {error && <div className="message error">{error}</div>}
      {successMessage && <div className="message success">{successMessage}</div>}

      <div className="card">
        <form className="inline" onSubmit={(e) => e.preventDefault()} style={{ marginBottom: 0 }}>
          <input
            type="text"
            placeholder="Search by project name..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ maxWidth: '240px' }}
          />
          <input
            type="number"
            placeholder="Min score"
            value={minScore}
            onChange={(e) => setMinScore(e.target.value)}
            style={{ maxWidth: '110px' }}
          />
          <input
            type="number"
            placeholder="Max score"
            value={maxScore}
            onChange={(e) => setMaxScore(e.target.value)}
            style={{ maxWidth: '110px' }}
          />
          {(searchTerm || minScore || maxScore) && (
            <button
              type="button"
              className="secondary"
              onClick={() => { setSearchTerm(''); setMinScore(''); setMaxScore(''); }}
            >
              Clear filters
            </button>
          )}
        </form>
      </div>

      {filteredHistory.length === 0 && <p className="muted-hint">No matching reviews found.</p>}

      <ul className="project-list">
        {filteredHistory.map((h) => (
          <li key={h.reviewId} className="project-item">
            <div>
              <div className="project-info">
                <Link to={`/review/${h.reviewId}`}>{h.projectName}</Link>
                <span className="tag">Score: {h.score}</span>
              </div>
              <div className="project-meta">{new Date(h.createdAt).toLocaleString()}</div>
            </div>
            <div className="action-group">
              <button
                className="danger"
                onClick={() => handleDelete(h.reviewId)}
                disabled={deletingId === h.reviewId}
              >
                {deletingId === h.reviewId ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default ReviewHistory;