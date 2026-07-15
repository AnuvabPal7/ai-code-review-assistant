import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../services/api';

function ReviewResults() {
  const { reviewId } = useParams();
  const [findings, setFindings] = useState([]);
  const [complexity, setComplexity] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [findingsRes, complexityRes] = await Promise.all([
          api.get(`/reviews/${reviewId}/findings`),
          api.get(`/reviews/${reviewId}/complexity`),
        ]);
        setFindings(findingsRes.data);
        setComplexity(complexityRes.data);
      } catch (err) {
        setError('Failed to load review data');
      }
    };
    fetchData();
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
      <div className="nav-bar">
        <h2>Review Results</h2>
        <Link to="/dashboard">Back to Dashboard</Link>
        <button className="secondary" onClick={handleExportPdf}>Export as PDF</button>
      </div>

      {error && <div className="message error">{error}</div>}

      {complexity && (
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Complexity Analysis</h3>
          <div className="complexity-grid">
            <div className="complexity-stat">
              <div className="label">Classes</div>
              <div className="value">{complexity.numClasses}</div>
            </div>
            <div className="complexity-stat">
              <div className="label">Methods</div>
              <div className="value">{complexity.numMethods}</div>
            </div>
            <div className="complexity-stat">
              <div className="label">Lines of Code</div>
              <div className="value">{complexity.linesOfCode}</div>
            </div>
            <div className="complexity-stat">
              <div className="label">Cyclomatic Complexity</div>
              <div className="value">{complexity.cyclomaticComplexity}</div>
            </div>
            <div className="complexity-stat">
              <div className="label">Maintainability Index</div>
              <div className="value">{complexity.maintainabilityIndex}/100</div>
            </div>
            <div className="complexity-stat time-complexity-box">
              <div className="label">Estimated Time Complexity (AI)</div>
              <div className="value">{complexity.estimatedTimeComplexity}</div>
              <div className="explanation">{complexity.timeComplexityExplanation}</div>
            </div>
          </div>
        </div>
      )}

      <h3>Findings</h3>
      <table>
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
              <td><span className={`badge ${f.severity}`}>{f.severity}</span></td>
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