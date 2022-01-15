# RaceAssist

## Commands

### Audience

/raceassist audience join [RaceID]  観客に自分を追加します<br>
/raceassist audience leave [RaceID] 観客から自分を削除します<br>
/raceassist audience list [RaceID]  観客の一覧を表示<br>

### Bet

/raceassist bet can [RaceID] on/off 対象のレースに対して賭けが可能か変更します /raceassist bet delete [RaceID]        賭けを削除します /raceassist bet list [RaceID]賭けの一覧を表示します
/raceassist bet open [RaceID]          賭けをすることのできる画面を開くことができます /raceassist bet rate [RaceID]          賭けのレートを変更します

### Place

/raceassist place reverse [RaceID]  レースの走行方向の向きを反転<br>
/raceassist place central [RaceID]  レースの中心点を設定<br>
/raceassist place degree [RaceID]  レースのゴールの角度を設定(立っている場所基準90度刻み)<br>
/raceassist place lap [RaceID] <lap>  レースのラップ数を指定<br>
/raceassist place set [RaceID] in|out レース場の内周、外周を指定<br>
/raceassist place finish 上記の設定の終了<br>

### Player

/raceassist player add [RaceID] [Player]  騎手を追加<br>
/raceassist player remove [RaceID]  騎手を削除<br>
/raceassist player delete [RaceID]  騎手をすべて削除<br>
/raceassist player list [RaceID]  騎手の一覧を表示<br>

### Race

/raceassist race start [RaceID]  レースを開始<br>
/raceassist race debug [RaceID]  レースのデバッグ<br>
/raceassist race stop [RaceID]  レースの停止<br>
/raceassist race create [RaceID]  レースの作成<br>
/raceassist race delete [RaceID]  レースの削除<br>

## About Use sheets API

pluginフォルダの中のRaceAssistフォルダに**credentials.json**を入れます 最初の賭けが行われるとコンソールにOAuthの認証画面が開かれるのでスプレッドシートの所有者が認証してください

### Config.ymlの設定

```yaml
Sheets:
  applicationName: 'RaceAssist'
  #applicationNameは好きな名前にしてください
  spreadsheetId: '******'
#spreadsheetIdはhttps://docs.google.com/spreadsheets/d/******/edit#gid=0の******の部分
```

### credentials.jsonの設定

基本はAPIのダウンロードしたものをそのままでもいいですがclient_secretを追加してください

```json
{
    "installed": {
        "client_id": "******.apps.googleusercontent.com",
        "project_id": "******",
        "auth_uri": "https://accounts.google.com/o/oauth2/auth",
        "token_uri": "https://oauth2.googleapis.com/token",
        "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
        "client_secret": "******",
        "redirect_uris": [
            "urn:ietf:wg:oauth:2.0:oob",
            "http://localhost"
        ]
    }
}
```