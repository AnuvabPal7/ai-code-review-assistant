import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

function ReviewHistory() {
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchHistory = async () => {
      try {
        const res = await api.get('/reviews/history');
        setHistory(res.data);
      } catch (err) {
        setError('Failed to load history');
      }
    };
    fetchHistory();
  }, []);

  return (
    <div>
      <Link to="/dashboard">Back to Dashboard</Link>
      <h2>Review History</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <ul>
        {history.map((h) => (
          <li key={h.reviewId}>
            <Link to={`/review/${h.reviewId}`}>
              {h.projectName} - Score: {h.score} - {new Date(h.createdAt).toLocaleString()}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default ReviewHistory;
