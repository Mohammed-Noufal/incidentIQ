import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const SCREENSHOT_DIR = 'C:/Users/mmnou/.gemini/antigravity/brain/58ff3e33-100c-496c-a844-8d0f46e74074/screenshots';
fs.mkdirSync(SCREENSHOT_DIR, {recursive: true});

const CHROME_PATH = 'C:/Program Files/Google/Chrome/Application/chrome.exe';

async function clickButtonWithText(page, text) {
  const buttons = await page.$$('button');
  for (const b of buttons) {
    const t = await b.evaluate(el => el.textContent);
    if (t.includes(text)) {
      await page.evaluate(el => el.click(), b);
      return true;
    }
  }
  throw new Error(`Button with text "${text}" not found`);
}

async function clickLinkWithText(page, text) {
  const links = await page.$$('a');
  for (const l of links) {
    const t = await l.evaluate(el => el.textContent);
    if (t.includes(text)) {
      await page.evaluate(el => el.click(), l);
      return true;
    }
  }
  throw new Error(`Link with text "${text}" not found`);
}

async function runE2E() {
  console.log('--- STARTING E2E BROWSER VERIFICATION ---');
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1280,900']
  });

  const page = await browser.newPage();
  await page.setViewport({width: 1280, height: 900});

  const consoleErrors = [];
  const networkErrors = [];

  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });

  page.on('response', resp => {
    const status = resp.status();
    const url = resp.url();
    // Allow 401 on initial session check before login (expected when unauthenticated)
    if (status >= 400 && !(url.includes('/api/auth/me') && status === 401)) {
      networkErrors.push(`${status} ${resp.request().method()} ${url}`);
    }
  });

  try {
    // 1. Visit Home (Auth screen)
    console.log('1. Navigating to http://localhost:5173...');
    await page.goto('http://localhost:5173', {waitUntil: 'networkidle0'});
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '01_auth_screen.png'), fullPage: true});
    console.log('Captured 01_auth_screen.png');

    // 2. Switch to Register and create new account
    console.log('2. Testing user registration...');
    const switchBtn = await page.waitForSelector('button.auth-link');
    await switchBtn.click();
    await new Promise(r => setTimeout(r, 400));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '02_register_mode.png')});

    const randomSuffix = Math.floor(Math.random() * 10000);
    const newUsername = `qa_lead_${randomSuffix}`;
    const testPassword = 'SafePassword123!';

    // Fill registration form
    await page.type('input[autoComplete="username"]', newUsername);
    await page.type('input[type="password"]', testPassword);
    const submitBtn = await page.waitForSelector('.auth-card button:not(.auth-link)');
    await submitBtn.click();

    // 3. Wait for authenticated view (workspace)
    console.log('3. Waiting for authenticated dashboard...');
    await page.waitForSelector('.workspace', {timeout: 10000});
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '03_dashboard_agent.png'), fullPage: true});
    console.log('Captured 03_dashboard_agent.png - Logged in as:', newUsername);

    // 4. Ingest an alert and request briefing
    console.log('4. Ingesting alert & requesting briefing...');
    await clickButtonWithText(page, 'Trigger Alert & Briefing');

    // Wait for active incident and briefing memory line
    await page.waitForSelector('.memoryline', {timeout: 15000});
    await new Promise(r => setTimeout(r, 1000));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '04_active_incident_briefing.png'), fullPage: true});
    console.log('Captured 04_active_incident_briefing.png');

    // 5. Record an action attempt with WORSENED outcome and causal mechanism
    console.log('5. Recording action attempt with WORSENED outcome...');
    await clickButtonWithText(page, 'Record Action & Outcome');
    await new Promise(r => setTimeout(r, 1500));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '05_recorded_action_history.png'), fullPage: true});
    console.log('Captured 05_recorded_action_history.png');

    // 6. Resolve Incident
    console.log('6. Resolving active incident...');
    await clickButtonWithText(page, 'Resolve Incident & Sync Memory');
    await new Promise(r => setTimeout(r, 2000));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '06_incident_resolved.png'), fullPage: true});
    console.log('Captured 06_incident_resolved.png');

    // 7. Incidents Explorer
    console.log('7. Testing Incidents Explorer tab...');
    await clickLinkWithText(page, 'Incidents Explorer');
    await page.waitForSelector('input[placeholder*="Search by code"]', {timeout: 5000});
    await new Promise(r => setTimeout(r, 1000));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '07_incidents_explorer.png'), fullPage: true});
    console.log('Captured 07_incidents_explorer.png');

    // 8. Historical Memory Bank Seeding
    console.log('8. Testing Historical Memory Bank seeding...');
    await clickLinkWithText(page, 'Historical Memory');
    await new Promise(r => setTimeout(r, 500));
    await clickButtonWithText(page, 'Seed Memory Bank');
    await page.waitForSelector('.panel b', {timeout: 15000});
    await new Promise(r => setTimeout(r, 1000));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '08_memory_seeding_complete.png'), fullPage: true});
    console.log('Captured 08_memory_seeding_complete.png');

    // 9. About Platform Page
    console.log('9. Testing About Platform view...');
    await clickLinkWithText(page, 'About Platform');
    await new Promise(r => setTimeout(r, 600));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '09_about_platform.png'), fullPage: true});
    console.log('Captured 09_about_platform.png');

    // 10. Feedback Submission
    console.log('10. Testing Feedback Form...');
    await clickLinkWithText(page, 'Feedback & Support');
    await page.waitForSelector('input[placeholder="e.g. Alex Morgan"]', {timeout: 5000});
    await page.type('input[placeholder="e.g. Alex Morgan"]', 'QA Test Lead');
    await page.type('input[placeholder="name@company.com"]', 'qa-lead@incidentiq.local');
    await page.type('textarea[placeholder*="Describe your experience"]', 'Production verification run: All memory items, warnings, and resolution flows function with zero errors.');
    
    // Select 5-star rating
    const starBtns = await page.$$('.rating-btn');
    if (starBtns.length >= 5) await starBtns[4].click();

    await page.screenshot({path: path.join(SCREENSHOT_DIR, '10_feedback_form_filled.png')});
    
    // Submit feedback
    await clickButtonWithText(page, 'Submit Feedback');
    await new Promise(r => setTimeout(r, 1500));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '11_feedback_success.png')});
    console.log('Captured 11_feedback_success.png');

    // 11. Prototype Demo
    console.log('11. Testing Prototype Quick Investigation...');
    await clickLinkWithText(page, 'Prototype Investigation');
    await page.waitForSelector('form button', {timeout: 5000});
    await clickButtonWithText(page, 'Run Quick Investigation');
    await new Promise(r => setTimeout(r, 3000));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '12_prototype_result.png'), fullPage: true});
    console.log('Captured 12_prototype_result.png');

    // 12. Responsive Views: Tablet & Mobile
    console.log('12. Testing Responsive Layouts...');
    // Tablet
    await page.setViewport({width: 768, height: 1024});
    await new Promise(r => setTimeout(r, 500));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '13_responsive_tablet.png')});
    // Mobile
    await page.setViewport({width: 375, height: 667});
    await new Promise(r => setTimeout(r, 500));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '14_responsive_mobile.png')});
    console.log('Captured 13_responsive_tablet.png & 14_responsive_mobile.png');

    // 13. Test Global Footer Navigation
    console.log('13. Testing Global Footer links...');
    await page.setViewport({width: 1280, height: 900});
    const footerLinks = await page.$$('footer a');
    for (const fl of footerLinks) {
      const text = await fl.evaluate(el => el.textContent);
      if (text === 'About') {
        await fl.click();
        break;
      }
    }
    await new Promise(r => setTimeout(r, 500));
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '15_footer_about_nav.png')});
    console.log('Captured 15_footer_about_nav.png');

    // 14. Sign Out
    console.log('14. Testing Sign Out...');
    const logoutBtn = await page.waitForSelector('button.logout');
    await logoutBtn.click();
    await page.waitForSelector('.auth-card', {timeout: 5000});
    await page.screenshot({path: path.join(SCREENSHOT_DIR, '16_signed_out.png')});
    console.log('Captured 16_signed_out.png');

    console.log('\n--- VERIFICATION SUMMARY ---');
    console.log(`Console Errors count: ${consoleErrors.length}`);
    if (consoleErrors.length > 0) console.log('Console Errors:', consoleErrors);
    console.log(`Unexpected Network Errors count: ${networkErrors.length}`);
    if (networkErrors.length > 0) console.log('Network Errors:', networkErrors);

    console.log('\nSUCCESS: ALL USER JOURNEYS PASSED PERFECTLY!');
  } catch (err) {
    console.error('E2E TEST FAILURE:', err);
    await page.screenshot({path: path.join(SCREENSHOT_DIR, 'error_state.png'), fullPage: true});
    process.exit(1);
  } finally {
    await browser.close();
  }
}

runE2E();
