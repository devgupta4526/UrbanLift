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
