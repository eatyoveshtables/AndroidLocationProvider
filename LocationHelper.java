package com.icebreakers.sss.logic;

import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationHelper
{
   private static final float ACCURACY_RADIUS = 2000; //68% confidence that user is within radius (meters)	
   private static final int TIMEOUT = 10000; //in milliseconds
   private static Context context = null;
   
   public LocationHelper(Context context)
   {
	   LocationHelper.context = context; 
   }
   
   public Location getUserLocation()
   {
	   LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	   Location lastKnownLocation = getBestPreviouslyStoredLocation(locationManager);
	   if(lastKnownLocation != null)
	   {
		   long millisSinceLastKnownLocation = System.currentTimeMillis() - lastKnownLocation.getTime();
		   long millisInOneHour = (60 * 60 * 1000) * (1);
		   if((lastKnownLocation.hasAccuracy()) && (lastKnownLocation.getAccuracy() < ACCURACY_RADIUS) 
		                                        && (millisSinceLastKnownLocation <= millisInOneHour))
		   {
			   return lastKnownLocation;
		   }
	   }
	   
	   /*
	   Location currentLocation = determineCurrentLocation(locationManager);
	   if(currentLocation != null)
	   {
		   return currentLocation; 
	   }
	   */
	   
	   return lastKnownLocation;   
   }
   
   private Location determineCurrentLocation(LocationManager locationManager)
   {
	   
	   Object syncObject = new Object();  
	   VenueLocationListener venueLocationListenerGPS = new VenueLocationListener();
	   VenueLocationListener venueLocationListenerNetwork = new VenueLocationListener();
	   VenueLocationListener venueLocationListenerPassive = new VenueLocationListener(); 
	   
	   boolean locationManagerReceivingUpdates = false; 
	   if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
	   {
		   locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, venueLocationListenerNetwork);
		   venueLocationListenerNetwork.setSyncObject(syncObject);
		   locationManagerReceivingUpdates = true; 
	   }
	   if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
	   {
		   locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, venueLocationListenerGPS);
		   venueLocationListenerGPS.setSyncObject(syncObject);
		   locationManagerReceivingUpdates = true; 
	   }
	   if(locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
	   {
		   locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, venueLocationListenerPassive);
		   venueLocationListenerPassive.setSyncObject(syncObject);
		   locationManagerReceivingUpdates = true; 
	   }
	   
	   if(locationManagerReceivingUpdates)
	   {
		   synchronized(syncObject)
		   {
			   try
			   {
				   syncObject.wait(TIMEOUT);
			   }
			   catch(Exception e){}
		   }
		   
		   locationManager.removeUpdates(venueLocationListenerPassive);
		   locationManager.removeUpdates(venueLocationListenerNetwork);
		   locationManager.removeUpdates(venueLocationListenerGPS);
		   
		   Location gpsLocation = venueLocationListenerGPS.getLocation();
		   Location networkLocation = venueLocationListenerNetwork.getLocation();
		   Location passiveLocation = venueLocationListenerPassive.getLocation();
		   
		   if(gpsLocation != null)
		   {
			   return gpsLocation;
		   }
		   else if(networkLocation != null)
		   {
			   return networkLocation; 
		   }
		   else if(passiveLocation != null)
		   {
			   return passiveLocation; 
		   }
	   }
	   
	   return null; 
	   
   }
   
   /*
   private static Location determineCurrentLocationGooglePlay()
   {
	   
	   
	   final LocationClient locationClient;
	   
	   GooglePlayServicesClient.ConnectionCallbacks connectionCallbacks = new GooglePlayServicesClient.ConnectionCallbacks()
	   {
		   public void onConnected(Bundle connectionHint)
		   {
			   locationClient.requestLocationUpdates(LocationRequest.create(), 
					   ;
		   }
		   
		   public void onDisconnected(){}
	   };
	   GooglePlayServicesClient.OnConnectionFailedListener connectionFailedListener = new GooglePlayServicesClient.OnConnectionFailedListener()
	   {
		   public void onConnectionFailed(ConnectionResult result)
		   {

		   }
	   };
	   
	   locationClient = new LocationClient(context, connectionCallbacks, connectionFailedListener); 
	   
	   return null;
   }
   */
	   
   private Location getBestPreviouslyStoredLocation(LocationManager locationManager)
   {
	   List<String> providers = locationManager.getAllProviders();
	   Location lastKnownLocation = null;
	   long timeOfLastKnownLocation = 0;
	   for(String provider : providers)
	   {
		   try
		   {
			   Location providerLocation = locationManager.getLastKnownLocation(provider);
			   
			   if(providerLocation.getTime() > timeOfLastKnownLocation)
			   {
				   lastKnownLocation = providerLocation; 
				   timeOfLastKnownLocation = lastKnownLocation.getTime();
			   }
		   }
		   catch (Exception e)
		   {
			   //don't have permission to access provider
		   }
	   }
	   return lastKnownLocation;
   }
   
   public static double calculateDistance(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude)
   {
	   double theta = fromLongitude - toLongitude;
	   double dist = Math.sin(convertFromDegtoRad(fromLatitude)) * 
			   		 Math.sin(convertFromDegtoRad(toLatitude)) + Math.cos(convertFromDegtoRad(fromLatitude)) * 
			   		 Math.cos(convertFromDegtoRad(toLatitude)) * Math.cos(convertFromDegtoRad(theta));
	   dist = Math.acos(dist);

	   return dist;
   }
   
   private static Double convertFromDegtoRad(double value)
   {
	   return (value * Math.PI / 180.0);
   }
   
   
   private class VenueLocationListener implements LocationListener
   {
	   private Object syncObject = null;
	   private Location currentLocation = null;
	   public void onLocationChanged(Location location) {
		   
		   if(location.hasAccuracy() && (location.getAccuracy() <= (ACCURACY_RADIUS)))
		   {
			   currentLocation = location; 
			   synchronized(syncObject){
				   syncObject.notify();
			   }
		   }
	   }
	   
	   public void setSyncObject(Object syncObject)
	   {
		   this.syncObject = syncObject; 
	   }
	   public Location getLocation()
	   {
		   return currentLocation; 
	   }
	   public void onStatusChanged(String provider, int status, Bundle extras) {}
	   public void onProviderEnabled(String provider) {}
	   public void onProviderDisabled(String provider) {}
	   
   }
   
}