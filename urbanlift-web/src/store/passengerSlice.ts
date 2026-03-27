import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { FareEstimateDto } from '@/lib/api';

export type PassengerHubView =
  | 'home'
  | 'plan'
  | 'price'
  | 'ride'
  | 'activity'
  | 'account'
  | 'pay'
  | 'rate';

export interface PassengerPersistState {
  hubView: PassengerHubView;
  trackedBookingId: number | null;
  pickupPlaceId: string;
  pickupCustom: { lat: number; lng: number } | null;
  dropPlaceId: string;
  fareResult: FareEstimateDto | null;
  selectedCarType: string;
  payBookingId: number | null;
  payAmount: number | null;
}

const initialState: PassengerPersistState = {
  hubView: 'home',
  trackedBookingId: null,
  pickupPlaceId: 'cp',
  pickupCustom: null,
  dropPlaceId: 'cyber',
  fareResult: null,
  selectedCarType: 'SEDAN',
  payBookingId: null,
  payAmount: null,
};

const passengerSlice = createSlice({
  name: 'passenger',
  initialState,
  reducers: {
    setHubView(state, action: PayloadAction<PassengerHubView>) {
      state.hubView = action.payload;
    },
    setTrackedBookingId(state, action: PayloadAction<number | null>) {
      state.trackedBookingId = action.payload;
    },
    setPickupPlaceId(state, action: PayloadAction<string>) {
      state.pickupPlaceId = action.payload;
    },
    setPickupCustom(state, action: PayloadAction<{ lat: number; lng: number } | null>) {
      state.pickupCustom = action.payload;
    },
    setDropPlaceId(state, action: PayloadAction<string>) {
      state.dropPlaceId = action.payload;
    },
    setFareResult(state, action: PayloadAction<FareEstimateDto | null>) {
      state.fareResult = action.payload;
    },
    setSelectedCarType(state, action: PayloadAction<string>) {
      state.selectedCarType = action.payload;
    },
    setPayDraft(state, action: PayloadAction<{ bookingId: number | null; amount: number | null }>) {
      state.payBookingId = action.payload.bookingId;
      state.payAmount = action.payload.amount;
    },
  },
});

export const {
  setHubView,
  setTrackedBookingId,
  setPickupPlaceId,
  setPickupCustom,
  setDropPlaceId,
  setFareResult,
  setSelectedCarType,
  setPayDraft,
} = passengerSlice.actions;

export default passengerSlice.reducer;
