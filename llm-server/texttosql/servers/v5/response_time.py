import re
import os
import math


def analyze_response_times(file_path="test_results.txt"):
    if not os.path.exists(file_path):
        print(f"Hata: {file_path} dosyası bulunamadı!")
        return

    response_times = []

    # Düzenli ifade (Regex) ile 'Yanıt Süresi: X.XXXX saniye' kalıbını yakalıyoruz
    time_pattern = re.compile(r"Yanıt Süresi:\s*([0-9.]+)\s*saniye")

    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            match = time_pattern.search(line)
            if match:
                # Bulunan süreyi float tipine çevirip listeye ekle
                response_times.append(float(match.group(1)))

    if not response_times:
        print(f"Dosya okundu ancak geçerli bir 'Yanıt Süresi' verisi bulunamadı.")
        return

    # İstatistiksel Hesaplamalar
    total_tests = len(response_times)
    total_time = sum(response_times)
    avg_time = total_time / total_tests
    min_time = min(response_times)
    max_time = max(response_times)

    # Standart Sapma Hesaplama (Modelin yanıt süreleri ne kadar tutarlı?)
    variance = sum((x - avg_time) ** 2 for x in response_times) / total_tests
    std_dev = math.sqrt(variance)

    # Sonuçları Ekrana Yazdırma
    print("=" * 40)
    print("⚡ YANIT SÜRESİ PERFORMANS ANALİZİ ⚡")
    print("=" * 40)
    print(f"Toplam Analiz Edilen Test : {total_tests} adet")
    print(f"Toplam Geçen Süre         : {total_time:.2f} saniye")
    print("-" * 40)
    print(f"🚀 ORTALAMA YANIT SÜRESİ  : {avg_time:.4f} saniye")
    print(f"⏱️  En Hızlı Yanıt         : {min_time:.4f} saniye")
    print(f"🐢 En Yavaş Yanıt         : {max_time:.4f} saniye")
    print(f"📊 Standart Sapma         : ±{std_dev:.4f} saniye")
    print("=" * 40)


if __name__ == "__main__":
    analyze_response_times()