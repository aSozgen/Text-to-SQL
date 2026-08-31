import json
import random

import requests
import time
import os
from tqdm import tqdm

# API endpoint'in (Sunucunun POST /predict kabul ettiğinden emin ol)
API_URL = "http://localhost:8000/predict"

# Spider test verisetinin yolu
DATASET_PATH = "spider/qwen_7b_test.jsonl"

# Çıktı Dosyaları
OUTPUT_SQL_FILE = "predictions.sql"
OUTPUT_TXT_FILE = "test_results.txt"
FAILED_JSON_FILE = "failed_predictions.json"

def evaluate_model():
    if not os.path.exists(DATASET_PATH):
        print(f"Hata: {DATASET_PATH} dosyası bulunamadı!")
        return

    # JSONL dosyasını doğru şekilde satır satır yükleme
    all_data = []
    with open(DATASET_PATH, 'r', encoding='utf-8') as f:
        for line in f:
            if line.strip():
                all_data.append(json.loads(line))

    total_in_file = len(all_data)
    print(f"Dosyada toplam {total_in_file} soru bulundu.")

    # --- RASTGELE 1000 TANESİNİ SEÇME ---
    # Her çalıştırmada aynı rastgele 1000 sorunun gelmesi için seed sabitliyoruz.
    # Eğer her seferinde tamamen farklı 1000 soru gelsin istersen aşağıdaki satırı silebilirsin.
    random.seed(42)

    sample_size = min(500, total_in_file)  # Dosyada 1000'den az veri varsa hata vermemesi için koruma
    test_data = random.sample(all_data, sample_size)
    total_queries = len(test_data)
    print(f"Bunlar arasından rastgele {total_queries} tanesi test için seçildi.\n")
    # İlk 20 veriyi test etmek istiyorsan burayı aktif bırakabilirsin:

    total_queries = len(test_data)
    if total_queries == 0:
        print("Dosyada test edilecek veri bulunamadı.")
        return

    successful_matches = 0
    failed_queries = []

    print(f"Toplam {total_queries} test verisi değerlendiriliyor...\n")

    # Çıktı dosyalarını temiz bir şekilde açalım
    with open(OUTPUT_SQL_FILE, "w", encoding="utf-8") as sql_file, \
         open(OUTPUT_TXT_FILE, "w", encoding="utf-8") as txt_file:

        # tqdm ile ilerleme çubuğu
        for idx, item in enumerate(tqdm(test_data, desc="Değerlendiriliyor"), 1):
            question = item['question']
            gold_sql = item['query']
            schema_content = item['schema']

            payload = {
                "question": question,
                "schema": schema_content
            }

            start_time = time.time()  # Süre ölçümü başlat
            try:
                response = requests.post(API_URL, json=payload, timeout=300)
                response_time = time.time() - start_time  # Süre ölçümü bitir

                if response.status_code == 200:
                    result = response.json()
                    generated_sql = result.get('sql', '').strip().replace("Query: ", "").strip()

                    # 1. SQL Dosyasına Sadece Çıktıyı Yaz (Exact Match için temiz format)
                    sql_file.write(f"-- Soru {idx}: {question}\n")
                    sql_file.write(f"{generated_sql}\n\n")

                    # 2. TXT Dosyasına Tüm Detayları ve Response Time Bilgisini Yaz
                    txt_file.write(f"=== TEST {idx} ===\n")
                    txt_file.write(f"Soru: {question}\n")
                    txt_file.write(f"Beklenen (Gold): {gold_sql}\n")
                    txt_file.write(f"Üretilen: {generated_sql}\n")
                    txt_file.write(f"Yanıt Süresi: {response_time:.4f} saniye\n")
                    txt_file.write("-" * 40 + "\n")

                    # Başarı Kontrolü (Exact Match)
                    if generated_sql.lower().replace(" ", "") == gold_sql.lower().replace(" ", ""):
                        successful_matches += 1
                    else:
                        failed_queries.append({
                            "id": idx,
                            "question": question,
                            "expected": gold_sql,
                            "generated": generated_sql,
                            "response_time_secs": response_time
                        })
                else:
                    error_msg = f"API Hatası (Kod: {response.status_code}): {response.text}"
                    tqdm.write(f"\n[HATA] Satır {idx}: {error_msg}")
                    txt_file.write(f"=== TEST {idx} API HATASI ===\nSoru: {question}\n{error_msg}\n" + "-" * 40 + "\n")
                    sql_file.write(f"-- TEST {idx} HATA: API istek başarısız.\n\n")

            except Exception as e:
                tqdm.write(f"\nİstek başarısız oldu: {e}")
                txt_file.write(f"=== TEST {idx} BAGLANTI HATASI ===\nSoru: {question}\nHata: {e}\n" + "-" * 40 + "\n")
                sql_file.write(f"-- TEST {idx} BAGLANTI HATASI\n\n")

            # Sistemi yormamak için ufak bekleme
            time.write_delay = time.sleep(0.1)

    # Sonuçları Hesapla ve Yazdır
    accuracy = (successful_matches / total_queries) * 100

    summary_results = (
        "\n" + "="*40 +
        "\nDEĞERLENDİRME SONUÇLARI" +
        "\n" + "="*40 +
        f"\nToplam Test     : {total_queries}" +
        f"\nBaşarılı        : {successful_matches}" +
        f"\nBaşarısız       : {total_queries - successful_matches}" +
        f"\nDoğruluk Oranı  : %{accuracy:.2f}\n"
    )

    print(summary_results)

    # Özet sonucu txt dosyasının en başına veya sonuna ekleyebilirsin, sonuna ekleyelim:
    with open(OUTPUT_TXT_FILE, "a", encoding="utf-8") as txt_file:
        txt_file.write("\n\n" + summary_results)

    # Hatalı olanları JSON olarak kaydet
    with open(FAILED_JSON_FILE, 'w', encoding='utf-8') as f:
        json.dump(failed_queries, f, ensure_ascii=False, indent=4)

    print(f"-> Üretilen SQL kodları '{OUTPUT_SQL_FILE}' dosyasına kaydedildi.")
    print(f"-> Detaylı rapor ve süreler '{OUTPUT_TXT_FILE}' dosyasına eklendi.")
    print(f"-> Hatalı tahminler '{FAILED_JSON_FILE}' dosyasına kaydedildi.")

if __name__ == "__main__":
    evaluate_model()