package cn.edu.gdmec.s07150704.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;


public class MainActivity extends AppCompatActivity {
        private MapView mapView;
        private BaiduMap baiduMap;
        //标注
        private Marker markerA;
        //信息窗口
        private InfoWindow infoWindow;
        //POI搜索
        private PoiSearch mPoiSearch;
        //机电经纬度坐标
        private LatLng dajidian = new LatLng(23.3906, 113.4535);

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //在使用SDK各组件之前初始化context信息，传入ApplicationContext
            //注意该方法要再setContentView方法之前实现
            SDKInitializer.initialize(getApplicationContext());
            setContentView(R.layout.activity_main);
            mapView = (MapView) findViewById(R.id.bmapView);
            //获取BaiduMap对象
            baiduMap = mapView.getMap();
            //创建BaiduMap的点击监听
            baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    String hint = "纬度:" + latLng.latitude + "\n经度:" + latLng.longitude;
                    Toast.makeText(MainActivity.this, hint, Toast.LENGTH_SHORT).show();
                }

                @Override
                public boolean onMapPoiClick(MapPoi mapPoi) {
                    return false;
                }
            });
            //创建BaiduMap的标注点击监听
            baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(final Marker marker) {
                    if (marker != markerA) {
                        return false;
                    }
                    Button button = new Button(getApplicationContext());
                    button.setBackgroundResource(R.drawable.popup);
                    button.setText("更改位置");
                    button.setTextColor(Color.BLACK);
                    //获取标注的地理位置坐标
                    final LatLng ll = marker.getPosition();
                    //获取标注的屏幕坐标
                    Point p = baiduMap.getProjection().toScreenLocation(ll);
                    //信息窗口要比标注点高47像素
                    p.y -= 47;
                    LatLng llInfo = baiduMap.getProjection().fromScreenLocation(p);
                    //创建信息窗口点击监听
                    InfoWindow.OnInfoWindowClickListener listener = new InfoWindow.OnInfoWindowClickListener() {
                        @Override
                        public void onInfoWindowClick() {
                            LatLng llNew = new LatLng(ll.latitude + 0.005,
                                    ll.longitude + 0.005);
                            //把被点中的标注向左上角移动一点
                            marker.setPosition(llNew);
                            //隐藏信息窗口
                            baiduMap.hideInfoWindow();
                        }
                    };
                    //创建信息窗口
                    infoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(button), llInfo, -47, listener);
                    //显示信息窗口
                    baiduMap.showInfoWindow(infoWindow);
                    return false;
                }
            });
            // 初始化搜索模块，注册搜索事件监听
            mPoiSearch = PoiSearch.newInstance();
            //创建搜索结果监听
            OnGetPoiSearchResultListener getPoiSearchResultListener = new OnGetPoiSearchResultListener() {
                @Override    //获取POI搜索结果回调方法
                public void onGetPoiResult(PoiResult poiResult) {
                    //如果搜索结果出错就返回。
                    if (poiResult == null
                            || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                        return;
                    }
                    if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                        //清除地图上的标注
                        baiduMap.clear();
                        //创建自定义的标注对象
                        PoiOverlay overlay = new MyPoiOverlay(baiduMap);
                        //把标注的点击监听委托给自定义的标注对象。
                        baiduMap.setOnMarkerClickListener(overlay);
                        //把搜索结果集合传给自定义标注
                        overlay.setData(poiResult);
                        //把标注添加到地图上
                        overlay.addToMap();
                        //缩放地图，使所有标注都在合适的视野内
                        overlay.zoomToSpan();
                        Toast.makeText(
                                MainActivity.this,
                                "总共查到" + poiResult.getTotalPoiNum() + "个兴趣点, 分为"
                                        + poiResult.getTotalPageNum() + "页", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                @Override   //获取POI详细内容回调方法
                public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
                    if (poiDetailResult.error != SearchResult.ERRORNO.NO_ERROR) {
                        Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(MainActivity.this, poiDetailResult.getName() +
                                "\n你是吃货,鉴定完毕", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

                }
            };
            //POI搜索设置回调监听
            mPoiSearch.setOnGetPoiSearchResultListener(getPoiSearchResultListener);


        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            menu.add(0,1,0,"更改地图类型");
            menu.add(0,2,0,"飞到机电");
            menu.add(0,3,0,"标注覆盖物");
            menu.add(0,4,0,"标注文字");
            menu.add(0,5,0,"POI搜索");
            menu.add(0,6,0,"GPS定位");
            return super.onCreateOptionsMenu(menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()){
                case 1:
                    setMapType();
                    break;
                case 2:
                    setCenter(dajidian);
                    break;
                case 3:
                    setOverlay();
                    break;
                case 4:
                    setMapText();
                    break;
                case 5:
                    POISearch();
                    break;
                case 6:
                    GPSLocation();
                    break;
            }
            return super.onOptionsItemSelected(item);
        }
        private void setMapType(){
            if(baiduMap.getMapType()==BaiduMap.MAP_TYPE_SATELLITE){
                baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            }else{
                baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
            }
        }
        private void setCenter(LatLng latlng){
            //创建地图状态对象
            MapStatus mapStatus = new MapStatus.Builder().target(latlng).zoom(18).build();
            //创建改变地图状态对象
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
            baiduMap.setMapStatus(mapStatusUpdate);
        }
        private void setOverlay(){
            //创建图示对象
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher);
            //创建标注选项对象
            OverlayOptions overlayOptions = new MarkerOptions().position(dajidian).icon(bitmapDescriptor);
            //把标注对象添加到地图上
            markerA = (Marker) baiduMap.addOverlay(overlayOptions);
        }
        private void setMapText(){
            //创建文字标注选项对象
            OverlayOptions overlayOptions = new TextOptions().text("大机电").
                    fontColor(0xFFFF00FF).fontSize(24).bgColor(0xAAFFFF00).position(dajidian).rotate(45);
            //把文字标注对象添加到地图上
            baiduMap.addOverlay(overlayOptions);
        }
        private void POISearch(){
            //周边搜索餐厅
            mPoiSearch.searchNearby(new PoiNearbySearchOption().location(dajidian).keyword("餐厅").radius(1000).pageNum(1));
        }
        //自定义POI标注对象,重写onPoiClick方法,实现点击标注后自动搜索POI标注的详细信息
        private class MyPoiOverlay extends com.baidu.mapapi.overlayutil.PoiOverlay {

            public MyPoiOverlay(BaiduMap baiduMap) {
                super(baiduMap);
            }

            @Override
            public boolean onPoiClick(int index) {
                super.onPoiClick(index);
                PoiInfo poi = getPoiResult().getAllPoi().get(index);
                if (poi.hasCaterDetails) {
                    mPoiSearch.searchPoiDetail((new PoiDetailSearchOption()).poiUid(poi.uid));
                }
                return true;
            }
        }

        private void GPSLocation(){
            // 获取的是位置服务
            String serviceString = Context.LOCATION_SERVICE;
            // 调用getSystemService()方法来获取LocationManager对象
            LocationManager locationManager = (LocationManager) getSystemService(serviceString);
            // 指定LocationManager的定位方法
            String provider = LocationManager.GPS_PROVIDER;
            Location location = null;
            // 这段权限检查代码是Android studio自动生成的,不用自己手打。
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            //调用getLastKnownLocation()方法获取当前的位置信息
            location = locationManager.getLastKnownLocation(provider);
            // 产生位置改变事件的条件设定为距离改变10米，时间间隔为2秒，设定监听位置变化
            locationManager.requestLocationUpdates(provider, 2000, 10,locationListener);
        }
        //创建GPS定位监听
        private final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //地图中心为当前位置
                setCenter(new LatLng(location.getLatitude(),location.getLongitude()));
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }
            @Override
            public void onProviderEnabled(String s) {
            }
            @Override
            public void onProviderDisabled(String s) {
            }
        };

    }
