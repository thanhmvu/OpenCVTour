/*
 * Copyright 2015 Lafayette College
 *
 * This file is part of OpenCVTour.
 *
 * OpenCVTour is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCVTour is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenCVTour.  If not, see <http://www.gnu.org/licenses/>.
 */

package alicrow.opencvtour;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

/**
 * Created by daniel on 5/28/15.
 *
 * Wrapper around the Google LocationServices API.
 * Handles the details so that other classes can just connect to LocationService and query it for the last known location.
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
	private static final String TAG = "LocationService";

	private GoogleApiClient _google_api_client;
	private boolean _requesting_location_updates;
	private LocationRequest _location_request;
	private Location _current_location;
	private ArrayList<LocationUpdateListener> _listeners;
	private final IBinder _binder = new LocationServiceBinder();

	private void setupGoogleLocationServices() {
		_google_api_client = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
	}

	private void createLocationRequest() {
		_location_request = new LocationRequest();
		_location_request.setInterval(10000);
		_location_request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	public Location getCurrentLocation() {
		if(_current_location == null)
			_current_location = LocationServices.FusedLocationApi.getLastLocation(_google_api_client);

		return _current_location;
	}

	public void stopLocationUpdates() {
		if(!_google_api_client.isConnected()) {
			/// Not connected yet. No need to remove location updates.
			return;
		}
		LocationServices.FusedLocationApi.removeLocationUpdates(_google_api_client, this);
		Log.d(TAG, "stopping location updates");
	}
	public void startLocationUpdates() {
		if(!_google_api_client.isConnected()) {
			Log.w(TAG, "Google API client is not yet connected; cannot request updates yet");
			return;
		}
		LocationServices.FusedLocationApi.requestLocationUpdates(_google_api_client, _location_request, this);
		Log.d(TAG, "starting location updates");
	}

	@Override
	public void onCreate() {
		setupGoogleLocationServices();
		createLocationRequest();
		_requesting_location_updates = true;
		Log.i(TAG, "LocationService created");
		_google_api_client.connect();
		_listeners = new ArrayList<>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "LocationService destroyed");
		_google_api_client.disconnect();
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.i(TAG, "connected to Google Play services");
		if(_requesting_location_updates)
			startLocationUpdates();
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.e(TAG, "connection to Google Play services has been suspended");
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.e(TAG, "connection to Google Play services has failed");
	}

	public static class ServiceConnection implements android.content.ServiceConnection {
		private LocationService _service;

		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been established, giving us the service object we can use to interact with the service.  Because we have bound to a explicit service that we know is running in our own process, we can cast its IBinder to a concrete class and directly access it.
			_service = ((LocationService.LocationServiceBinder)service).getService();
			_service.startLocationUpdates();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never see this happen.
			_service = null;
		}
		public LocationService getService() {
			return _service;
		}
	}

	/**
	 * Class for clients to access.  Because we know this service always runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocationServiceBinder extends Binder {
		LocationService getService() {
			return LocationService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return _binder;
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.v(TAG, "received location update");
		_current_location = location;
		for(LocationUpdateListener l : _listeners)
			l.onLocationUpdated(location);
	}

	/// add/remove listeners which will be notified when the location changes. We're not using these right now (our activities are only interested in querying the location, not getting updates), but they could be useful in the future
	public void addListener(LocationUpdateListener l) {
		_listeners.add(l);
	}
	public void removeListener(LocationUpdateListener l) {
		_listeners.remove(l);
	}

	/**
	 * Interface for other classes that want to know when the Location is updated.
	 */
	public interface LocationUpdateListener {
		void onLocationUpdated(Location location);
	}
}


