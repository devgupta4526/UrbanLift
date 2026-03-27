/** Named locations for trip planning (coordinates only — no map SDK). */

export type RidePlace = {
  id: string;
  label: string;
  area: string;
  lat: number;
  lng: number;
};

export const RIDE_AREA_LABEL = 'Delhi NCR';

export const RIDE_PLACES: RidePlace[] = [
  {
    id: 'cp',
    label: 'Connaught Place',
    area: 'New Delhi',
    lat: 28.6315,
    lng: 77.2167,
  },
  {
    id: 'indiagate',
    label: 'India Gate',
    area: 'New Delhi',
    lat: 28.6129,
    lng: 77.2295,
  },
  {
    id: 'hauz',
    label: 'Hauz Khas Village',
    area: 'South Delhi',
    lat: 28.5494,
    lng: 77.2001,
  },
  {
    id: 'igi',
    label: 'Indira Gandhi Airport T3',
    area: 'Southwest Delhi',
    lat: 28.5562,
    lng: 77.1,
  },
  {
    id: 'cyber',
    label: 'Cyber Hub',
    area: 'Gurugram',
    lat: 28.4959,
    lng: 77.0887,
  },
  {
    id: 'noida',
    label: 'DLF Mall of India',
    area: 'Noida',
    lat: 28.5677,
    lng: 77.3215,
  },
  {
    id: 'karol',
    label: 'Karol Bagh',
    area: 'Central Delhi',
    lat: 28.6517,
    lng: 77.1909,
  },
  {
    id: 'dwarka',
    label: 'Dwarka Sector 21 Metro',
    area: 'Southwest Delhi',
    lat: 28.5522,
    lng: 77.0589,
  },
  {
    id: 'redfort',
    label: 'Red Fort (Lal Qila)',
    area: 'Old Delhi',
    lat: 28.6562,
    lng: 77.241,
  },
  {
    id: 'lotus',
    label: 'Lotus Temple',
    area: 'South Delhi',
    lat: 28.5535,
    lng: 77.2588,
  },
  {
    id: 'akshardham',
    label: 'Akshardham',
    area: 'East Delhi',
    lat: 28.6127,
    lng: 77.2773,
  },
  {
    id: 'select',
    label: 'Select Citywalk',
    area: 'Saket',
    lat: 28.5284,
    lng: 77.219,
  },
  {
    id: 'nehru',
    label: 'Nehru Place',
    area: 'South Delhi',
    lat: 28.5499,
    lng: 77.2514,
  },
  {
    id: 'rajiv',
    label: 'Rajiv Chowk Metro',
    area: 'Connaught Place',
    lat: 28.6328,
    lng: 77.2196,
  },
  {
    id: 'faridabad',
    label: 'Crown Interiorz Mall',
    area: 'Faridabad',
    lat: 28.4089,
    lng: 77.3178,
  },
];

/** Quick coords for driver live-location / socket demos (same region as rider presets). */
export const DEMO_STREAM_PRESETS: { id: string; label: string; lat: number; lng: number }[] = [
  { id: 'cp', label: 'Connaught Place', lat: 28.6315, lng: 77.2167 },
  { id: 'igi', label: 'IGI T3', lat: 28.5562, lng: 77.1 },
  { id: 'cyber', label: 'Cyber Hub', lat: 28.4959, lng: 77.0887 },
  { id: 'noida', label: 'Mall of India', lat: 28.5677, lng: 77.3215 },
  { id: 'redfort', label: 'Red Fort', lat: 28.6562, lng: 77.241 },
  { id: 'lotus', label: 'Lotus Temple', lat: 28.5535, lng: 77.2588 },
  { id: 'select', label: 'Select Citywalk', lat: 28.5284, lng: 77.219 },
  { id: 'faridabad', label: 'Faridabad mall', lat: 28.4089, lng: 77.3178 },
];

export function getPlace(id: string): RidePlace | undefined {
  return RIDE_PLACES.find((p) => p.id === id);
}

export const CAR_CLASS_LABELS: Record<string, string> = {
  SEDAN: 'Urban',
  HATCHBACK: 'Lite',
  SUV: 'Plus',
  COMPACT_SUV: 'Comfort',
  XL: 'XL',
};
