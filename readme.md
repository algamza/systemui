## StatusBar

System UI Plugin을 사용한 클래스 설계

### 1차 설계

![Figure 1](/docs/statusbar1.PNG)

- 컨트롤러와 뷰를 분리
- 컨트롤러에서 시스템 라이브러리 사용 ( 필요할 경우 컨트롤러에서 서비스, 쓰레드를 사용하여 라이브러리들과 통신 )

### 2차 설계

![Figure 2](/docs/statusbar2.PNG)

- UI 는 System UI Processor 에서 Control service는 statusbar remote processor에서 동작
- 두 processor 사이는 aidl interface를 통해 통신

## Droplist

### 설계
참조 Application 분석 ( Hvac )
- AOSP 참조 위치 : packages\apps\Car\Hvac ( https://android.googlesource.com/platform/packages/apps/Car/Hvac )
- class 관계도 : https://drive.google.com/file/d/1GUnXVnGv8E-dlK_82wLZI0ZewSQzuF5i/view?usp=sharing

Drop List 설계
- class diagram ( droplist.vsd )
- Broadcast Receiver로 부터 부팅 시점에 서비스를 시작
- UI 컨트롤을 위한 Main thread와 System 컨트롤을 위한 Sub thread
- UI 서비스의 각 컨트롤러에서 binder를 통해 System 서비스를 사용

![Figure 3](/docs/droplist1.PNG)

## Volume UI

### 설계
- 하나의 system 서비스로 boot completed 를 통해 시작 하도록 android.intent.action.BOOT_COMPLETED
- UI를 컨트롤 하는 VolumeDialogService와 Volume을 컨트롤 하는 VolumeControlService로 분리
- window type을 TYPE_VOLUME_OVERLAY로 하고, Dialog window 로 생성 show, hide등의 기능을 이용하기 위해
- CarAudioManagerEx를 통해 Volume Control
- ICarVolumeCallback을 통해 Car volume event
- 설계 문서 참고 : volumeUI.vsd

![Figure 4](/docs/volume1.PNG)

## Notification

### 설계 
- 시스템으로 부터 Notification을 받기 위한 Service를 NotificationListenerService를 상속 받아 생성
  - 참고 : https://developer.android.com/reference/android/service/notification/NotificationListenerService
- UI를 위한 NotificaitonUIService 생성
- 각 서비스간에는 LocalBroadcastManager를 통해 통신
- 각각의 서비스에는 task safety 를 위한 handler 사용

![Figure 5](/docs/notification1.PNG)

