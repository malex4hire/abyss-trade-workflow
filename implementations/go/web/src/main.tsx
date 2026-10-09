import { createRoot } from 'react-dom/client';
import { App } from './App';
import './style.css';

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('Portal root element is missing');

createRoot(rootElement).render(<App />);
