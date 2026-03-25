import { Routes, Route } from 'react-router-dom';
import { HomePage } from '@/pages/HomePage';
import { PassengerAuthPage } from '@/pages/passenger/PassengerAuthPage';
import { DriverAuthPage } from '@/pages/driver/DriverAuthPage';

export default function App() {
  return (
    <div className="min-h-screen bg-hero-glow bg-grid-fade bg-night-950">
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/passenger" element={<PassengerAuthPage />} />
        <Route path="/driver" element={<DriverAuthPage />} />
      </Routes>
    </div>
  );
}
