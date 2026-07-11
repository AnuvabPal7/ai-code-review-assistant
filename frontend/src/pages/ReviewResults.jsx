import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../services/api';

function ReviewResults() {
  const { reviewId } = useParams();
  const [findings, setFindings] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchFindings = async () => {
      try {
        const res = await api.get(`/reviews/${reviewId}/findings`);
        setFindings(res.data);
      } catch (err) {
        setError('Failed to load findings');
      }
    };
    fetchFindings();
  }, [reviewId]);

  const handleExportPdf = async () => {
    try {
      const res = await api.get(`/reviews/${reviewId}/export/pdf`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `review-${reviewId}-report.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      setError('PDF export failed');
    }
  };

  return (
    <div>
      <Link to="/dashboard">Back to Dashboard</Link>
      <h2>Review Results</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <button onClick={handleExportPdf}>Export as PDF</button>

      <table border="1" cellPadding="8" style={{ marginTop: '20px', width: '100%' }}>
        <thead>
          <tr>
            <th>Severity</th>
            <th>Source</th>
            <th>Type</th>
            <th>Issue</th>
            <th>Suggestion</th>
            <th>Line</th>
          </tr>
        </thead>
        <tbody>
          {findings.map((f) => (
            <tr key={f.id}>
              <td>{f.severity}</td>
              <td>{f.source}</td>
              <td>{f.findingType}</td>
              <td>{f.issue}</td>
              <td>{f.suggestion}</td>
              <td>{f.lineNumber}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default ReviewResults;
