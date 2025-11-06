
## 付録A. シーケンス図（アプリ起動〜終了〜クールダウン〜緊急解除）

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant TargetApp as 対象アプリ
    participant FocusGate as FocusGate(本アプリ)
    participant System as Androidシステム

    User->>TargetApp: アプリをタップして起動
    TargetApp-->>FocusGate: フォアグラウンドイベント検知(Accessibility)

    FocusGate->>User: Q1ポップアップ「何分使用しますか？」
    User-->>FocusGate: 分数入力→OK

    FocusGate->>User: Q2ポップアップ「何のために使用しますか？」
    User-->>FocusGate: 目的入力→OK

    FocusGate->>User: Q3確認ポップアップ「〇〇分××のために使用します」
    alt 同意(はい)
        FocusGate-->>System: HOME→対象アプリへ遷移許可
        FocusGate->>FocusGate: ForegroundService開始（タイマー起動）
        Note over FocusGate: セッションタイマー開始
        ...指定分経過...
        FocusGate->>User: 使用終了ポップアップ表示
        User-->>FocusGate: OK押下
        FocusGate-->>System: HOME遷移（対象アプリ閉鎖）
        FocusGate->>FocusGate: クールダウン開始（30分）
    else 同意しない(いいえ/キャンセル)
        FocusGate-->>System: HOME遷移
    end

    Note over FocusGate: クールダウン中、対象アプリ起動を監視

    User->>TargetApp: 再度起動を試みる（クールダウン中）
    FocusGate-->>User: ブロック画面表示「クールダウン中です 残りmm:ss」

    alt 緊急解除選択
        User-->>FocusGate: 「緊急解除」ボタン押下
        FocusGate->>User: PIN/生体認証
        User-->>FocusGate: 認証成功
        FocusGate->>User: 理由入力ポップアップ
        User-->>FocusGate: 理由入力→OK
        FocusGate->>FocusGate: 一時解除(15分間)
        Note over FocusGate: 一時解除中のみ対象アプリ起動許可
        ...15分経過...
        FocusGate->>FocusGate: 自動的に再ロック
    else 解除せず待機
        FocusGate->>User: カウントダウン表示継続
    end

    Note over FocusGate: クールダウン終了→通常状態へ復帰
```

