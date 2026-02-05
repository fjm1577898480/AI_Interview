import os
import time
import json
import datetime
import schedule
import re
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.common.exceptions import StaleElementReferenceException
from selenium.common.exceptions import TimeoutException
from selenium.common.exceptions import WebDriverException
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager

# --- 配置 ---
# 目标URL：Java面经搜索结果
TARGET_URL = "https://www.nowcoder.com/search/all?query=java&type=all&searchType=%E9%A1%B6%E9%83%A8%E5%AF%BC%E8%88%AA%E6%A0%8F&subType=818"
# 目标保存路径：直接保存到 Android 项目的 assets 目录，方便 App 读取
# 注意：如果您的脚本不在 scripts 文件夹下，可能需要调整这里的路径回退层级
OUTPUT_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app/src/main/assets/interview_data.json")
TARGET_COUNT = 200  # 目标抓取数量 (前10页大概150-200条)

# --- 辅助函数 ---

def get_driver():
    """初始化 Selenium WebDriver"""
    options = Options()
    # options.add_argument("--headless")  # 调试时可以注释掉这行，看到浏览器界面
    options.add_argument("--disable-gpu")
    options.add_argument("--no-sandbox")
    options.add_argument("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")

    # 尝试在当前脚本目录查找 chromedriver.exe
    current_dir = os.path.dirname(os.path.abspath(__file__))
    driver_path = os.path.join(current_dir, "chromedriver.exe")
    
    # 显式指定 Chrome 浏览器路径（根据你的日志路径）
    options.binary_location = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

    # --- 关键修改：使用临时目录，避免与主 Chrome 冲突 ---
    # 为了解决 DevToolsActivePort file doesn't exist 错误，我们不能直接复用主配置
    # 而是创建一个新的临时配置，虽然这需要重新登录一次，但能保证脚本运行稳定
    user_data_dir = os.path.join(os.environ["LOCALAPPDATA"], "Google\\Chrome\\User Data_Crawler")
    if not os.path.exists(user_data_dir):
        os.makedirs(user_data_dir)
        
    options.add_argument(f"--user-data-dir={user_data_dir}")
    # options.add_argument("--profile-directory=Default") 
    
    # 添加防崩溃参数
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    # 重新启用远程调试端口
    options.add_argument("--remote-debugging-port=9222")
    options.add_argument("--ignore-certificate-errors")
    options.add_argument("--ignore-ssl-errors")
    
    if os.path.exists(driver_path):
        print(f"使用本地驱动: {driver_path}")
        service = Service(driver_path)
        return webdriver.Chrome(service=service, options=options)
    else:
        print("未找到本地驱动，尝试使用系统 PATH 中的驱动...")
        # 直接使用系统安装的驱动
        return webdriver.Chrome(options=options)

def clean_text(text):
    """简单的文本清洗"""
    if not text:
        return ""
    t = re.sub(r"\u200b|\u200c|\u200d|\ufeff", "", text)
    t = re.sub(r"扫码下载牛客APP", "", t, flags=re.IGNORECASE)
    t = re.sub(r"共\s*\d+\s*张，最多还能上传\s*\d+\s*张", "", t)
    t = re.sub(r"\s+", " ", t).strip()
    return t

def categorize_post(title, content):
    """简单的关键词分类逻辑"""
    text = (title + content).lower()
    if any(k in text for k in ["算法", "leetcode", "dp", "二叉树"]):
        return "算法与数据结构"
    if any(k in text for k in ["jvm", "gc", "内存", "类加载"]):
        return "Java基础/JVM"
    if any(k in text for k in ["mysql", "redis", "数据库", "索引"]):
        return "数据库"
    if any(k in text for k in ["spring", "mybatis", "springboot", "框架"]):
        return "主流框架"
    if any(k in text for k in ["网络", "tcp", "http", "socket"]):
        return "计算机网络"
    return "综合面经"

def go_to_next_page(driver, next_page_num):
    """只在分页条区域内点击目标页码/下一页，并等待结果列表发生变化。"""
    print(f"尝试跳转到第 {next_page_num} 页...")

    def get_result_links(limit=6):
        hrefs = []
        els = driver.find_elements(By.XPATH, "//a[contains(@href, '/feed/main/detail')]")
        if not els:
            els = driver.find_elements(By.XPATH, "//a[contains(@href, '/discuss/')]")
        for el in els:
            try:
                href = el.get_attribute("href")
            except StaleElementReferenceException:
                continue
            if href:
                hrefs.append(href)
            if len(hrefs) >= limit:
                break
        return hrefs

    def find_pager():
        selectors = [
            ".el-pagination",
            ".pagination",
            ".pager",
            ".page-box",
            "[class*='pagination']"
        ]
        for sel in selectors:
            try:
                el = driver.find_element(By.CSS_SELECTOR, sel)
                if el and el.is_displayed():
                    return el
            except Exception:
                pass
        return None

    def wait_result_changed(before, timeout_sec=12):
        end = time.time() + timeout_sec
        while time.time() < end:
            time.sleep(0.5)
            after = get_result_links(limit=len(before) if before else 6)
            if after and after != before:
                return True
        return False

    def click_in_pager_by_text(pager, text_value):
        try:
            candidates = pager.find_elements(
                By.XPATH,
                f".//*[self::a or self::button or self::li or self::span][normalize-space(text())='{text_value}']"
            )
        except StaleElementReferenceException:
            return False

        for el in candidates:
            try:
                if not el.is_displayed():
                    continue
                target = el
                if target.tag_name.lower() not in ("a", "button"):
                    children = target.find_elements(By.XPATH, ".//a|.//button")
                    if children:
                        target = children[0]
                driver.execute_script("arguments[0].scrollIntoView({block: 'center'});", target)
                time.sleep(0.2)
                driver.execute_script("arguments[0].click();", target)
                return True
            except StaleElementReferenceException:
                continue
            except Exception:
                continue
        return False

    max_attempts = 3
    for attempt in range(max_attempts):
        try:
            before_links = get_result_links()

            driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
            time.sleep(1)

            pager = find_pager()
            if not pager:
                print("未找到分页条容器")
                return False

            if click_in_pager_by_text(pager, str(next_page_num)):
                wait_result_changed(before_links)
                return True

            pager = find_pager()
            if pager and click_in_pager_by_text(pager, "下一页"):
                wait_result_changed(before_links)
                return True

            pager = find_pager()
            if pager and click_in_pager_by_text(pager, ">"):
                wait_result_changed(before_links)
                return True

            if attempt < max_attempts - 1:
                time.sleep(0.8)
                continue

            try:
                pager = find_pager()
                if pager:
                    print("分页条HTML(截断):", pager.get_attribute("outerHTML")[:400])
            except Exception:
                pass

            print("未在分页条区域内找到可点击的翻页元素")
            return False

        except StaleElementReferenceException:
            if attempt < max_attempts - 1:
                time.sleep(0.8)
                continue
            print("翻页操作异常: stale element reference")
            return False
        except Exception as e:
            print(f"翻页操作异常: {e}")
            return False

def crawl_task():
    print(f"[{datetime.datetime.now()}] 开始执行爬取任务...")
    driver = get_driver()
    collected_links = set()
    posts_data = []

    try:
        # 1. 访问搜索页面
        driver.get(TARGET_URL)
        print("等待 20 秒，请在此期间手动完成登录...")
        time.sleep(20) # 等待页面首次加载

        # 尝试关闭登录弹窗
        try:
            close_btn = driver.find_element(By.CSS_SELECTOR, ".icon-close, .close-btn, [class*='close']")
            if close_btn:
                print("检测到登录弹窗，尝试关闭...")
                close_btn.click()
                time.sleep(1)
        except:
            pass # 如果没找到关闭按钮，忽略

        # 2. 遍历前10页获取链接
        # 注意：不再使用 URL 参数跳转，而是模拟点击“下一页”，因为部分网站 URL 参数可能无效
        for page in range(1, 11):
            print(f"正在处理第 {page}/10 页...")
            
            driver.execute_script("window.scrollTo(0, 0);")
            time.sleep(0.8)
            driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
            time.sleep(2)

            # 查找所有帖子链接
            elements = driver.find_elements(By.XPATH, "//a[contains(@href, '/feed/main/detail')]")
            if not elements:
                 elements = driver.find_elements(By.XPATH, "//a[contains(@href, '/discuss/')]")

            new_links_count = 0
            for el in elements:
                href = el.get_attribute("href")
                if href and href not in collected_links:
                    collected_links.add(href)
                    new_links_count += 1
            
            print(f"第 {page} 页新收集 {new_links_count} 个链接，总计: {len(collected_links)}")
            
            # 尝试翻页 (如果是最后一页则不需要)
            if page < 10:
                if not go_to_next_page(driver, page + 1):
                    print("无法继续翻页，停止链接收集。")
                    break

        # 3. 逐个访问链接抓取详情
        links_list = list(collected_links)[:TARGET_COUNT]
        print(f"开始抓取 {len(links_list)} 篇详情...")

        for index, link in enumerate(links_list):
            try:
                print(f"正在抓取 ({index + 1}/{len(links_list)}): {link}")

                loaded = False
                for _ in range(2):
                    try:
                        driver.get(link)
                        WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.TAG_NAME, "body")))
                        loaded = True
                        break
                    except (TimeoutException, WebDriverException):
                        time.sleep(1.2)
                if not loaded:
                    continue

                time.sleep(0.8)

                title = driver.title.replace("_牛客网", "").strip()

                raw_content = driver.execute_script("""
                    const candidates = [];
                    const selectors = [
                      "article",
                      "main",
                      "[class*='detail']",
                      "[class*='content']",
                      "[class*='post']",
                      "[class*='discuss']"
                    ];

                    for (const sel of selectors) {
                      document.querySelectorAll(sel).forEach(el => {
                        const t = (el && el.innerText) ? el.innerText.trim() : "";
                        if (t && t.length >= 80) candidates.push(t);
                      });
                    }

                    let best = "";
                    for (const t of candidates) {
                      if (t.length > best.length) best = t;
                    }

                    if (!best) {
                      best = Array.from(document.querySelectorAll('p'))
                        .map(p => (p.innerText || "").trim())
                        .filter(t => t.length > 5)
                        .join("\\n");
                    }

                    return best;
                """)

                cleaned_content = clean_text(raw_content)

                if len(cleaned_content) < 50:
                    continue

                summary = cleaned_content[:100] + "..."

                category = categorize_post(title, cleaned_content)

                post_id = link.split("/")[-1].split("?")[0]

                posts_data.append({
                    "id": post_id,
                    "title": title,
                    "link": link,
                    "category": category,
                    "summary": summary,
                    "content": cleaned_content,
                    "tags": ["Java", "校招", category],
                    "update_time": str(datetime.datetime.now().date())
                })

            except Exception as e:
                print(f"抓取 {link} 失败: {e}")
                continue

        # 4. 去重与保存
        # 如果文件已存在，读取旧数据进行合并去重
        if os.path.exists(OUTPUT_FILE):
            try:
                with open(OUTPUT_FILE, "r", encoding="utf-8") as f:
                    old_data = json.load(f)
                    old_ids = {item["id"] for item in old_data}
                    for new_item in posts_data:
                        if new_item["id"] not in old_ids:
                            old_data.append(new_item)
                    posts_data = old_data
            except:
                pass 
        
        # 确保目录存在 (会自动创建 app/src/main/assets)
        os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
        
        with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
            json.dump(posts_data, f, ensure_ascii=False, indent=2)
            
        print(f"抓取完成！共保存 {len(posts_data)} 条数据到 {OUTPUT_FILE}")

    except Exception as e:
        print(f"任务执行异常: {e}")
    finally:
        driver.quit()

# --- 调度逻辑 ---

def job_wrapper():
    """根据日期判断是否执行"""
    today = datetime.datetime.now()
    month = today.month
    
    # 招聘季：金三银四，金九银十
    is_recruitment_season = month in [3, 4, 9, 10]
    
    state_file = "crawler_state.json"
    should_run = False
    
    if is_recruitment_season:
        print("当前是招聘季，执行每日更新策略")
        should_run = True
    else:
        print("当前是日常时期，执行三天一更策略")
        last_run_date = None
        if os.path.exists(state_file):
            try:
                with open(state_file, "r") as f:
                    state = json.load(f)
                    last_run_date = datetime.datetime.fromisoformat(state.get("last_run"))
            except:
                pass
        
        if last_run_date is None or (today - last_run_date).days >= 3:
            should_run = True
        else:
            print("距离上次运行未满3天，跳过。")

    if should_run:
        crawl_task()
        with open(state_file, "w") as f:
            json.dump({"last_run": today.isoformat()}, f)

if __name__ == "__main__":
    # 开发测试模式：直接运行一次爬取
    crawl_task()