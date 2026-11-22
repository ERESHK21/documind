package com.documind.seed;

import com.documind.document.DocumentService;
import com.documind.user.Role;
import com.documind.user.User;
import com.documind.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds demo accounts and three small knowledge bases so the app is useful
 * on the first run. Every fact below is also used by the suggested questions
 * in the UI, so a demo answer always has a real citation.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final String PASSWORD = "Password123!";

    private final UserRepository users;
    private final DocumentService documents;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, DocumentService documents, PasswordEncoder encoder) {
        this.users = users;
        this.documents = documents;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }

        user("Admin", "admin@documind.dev", Role.ADMIN);
        User demo = user("Demo User", "demo@documind.dev", Role.USER);
        User riya = user("Riya Kapoor", "riya@documind.dev", Role.USER);

        documents.createFromText("Nimbus Notes - Product FAQ", """
                Nimbus Notes is a note-taking app for teams. This FAQ covers billing, exports,
                account recovery and support.

                Plans. The Free plan includes 3 notebooks and 100 MB of storage. The Pro plan
                costs $4 per month or $40 per year and includes unlimited notebooks, 50 GB of
                storage and version history for 90 days.

                Refunds. Refunds are available within 14 days of payment. Email
                billing@nimbusnotes.io with your order ID and the email on the account. Refunds
                are processed within 5 business days to the original payment method.

                Exports. You can export a notebook as PDF, Markdown or HTML from Settings >
                Export. Bulk export of all notebooks is available on the Pro plan only.

                Password reset. The password reset link expires after 30 minutes. If the email
                does not arrive, check the spam folder and then contact support.

                Offline mode. The desktop and mobile apps work offline. Changes sync
                automatically within 2 minutes after the device is back online.

                Limits. A single notebook can be at most 50 MB. Attachments are limited to
                20 MB per file.

                Support. Support hours are Monday to Friday, 9am to 6pm IST. Email
                support@nimbusnotes.io and expect a first reply within one business day.
                """, demo);

        documents.createFromText("Employee Handbook 2026", """
                This handbook summarises the working policies for full-time employees at
                Northwind Labs. It is reviewed every year by the People team.

                Working hours. Core hours are 9:30am to 6:30pm with one hour of flexibility on
                either side. Employees work 40 hours per week.

                Paid leave. Full-time employees receive 24 paid leave days per calendar year in
                addition to 10 public holidays. Leave requests must be submitted at least 5 days
                in advance. Sick leave is 10 days per year, and a medical certificate is required
                after 3 consecutive sick days.

                Remote work. Employees may work from home up to 2 days per week with manager
                approval. Fully remote arrangements require HR approval.

                Notice period. The notice period is 30 days for full-time employees and 60 days
                for managers. New employees serve a probation period of 90 days.

                Reimbursements. Submit expense claims within 30 days with receipts. Meals are
                capped at $50 per person per day. Travel must follow the separate travel policy.

                IT support. The helpdesk is available at helpdesk@northwind.example or extension
                204, Monday to Friday, 9am to 6pm.

                Performance reviews. Reviews happen twice a year, in April and October. Each
                review includes a self-assessment and a manager assessment.

                Conduct. Harassment and discrimination are not tolerated. Report concerns to
                hr@northwind.example. All reports are handled confidentially.
                """, demo);

        documents.createFromText("Security and Data Policy", """
                This policy explains how Northwind Labs protects company and customer data.

                Encryption. Data is encrypted at rest with AES-256. All traffic between clients
                and servers uses TLS 1.3. Keys are rotated every 90 days.

                Backups. Backups run every day at 03:00 UTC and are retained for 30 days. A
                restore test is performed every quarter and the result is recorded.

                Incident reporting. Report any suspected security incident within 1 hour to
                security@northwind.example. Do not discuss the incident publicly or on social
                media until the security team confirms it is resolved.

                Multi-factor authentication. MFA is mandatory for all staff from 1 January 2026.
                Accounts without MFA are disabled after the deadline.

                Passwords. Passwords must be at least 12 characters, rotated every 180 days, and
                stored only in the approved password manager. Reusing a company password on any
                other site is prohibited.

                Access reviews. Access rights are reviewed every quarter. The principle of least
                privilege applies, and access is removed within 24 hours when an employee leaves.

                Data classification. Data is classified as Public, Internal, Confidential or
                Restricted. Confidential and Restricted data must never be stored on personal
                devices or personal cloud accounts.

                Vendors. A security review is required before sharing any company data with a
                third-party vendor.
                """, demo);

        documents.createFromText("Travel Policy", """
                This policy applies to all employees travelling for company business.

                Booking. Book travel through TravelDesk at least 7 days before departure. Last
                minute bookings need manager approval.

                Flights. Use economy class for flights under 6 hours. Business class is allowed
                for flights longer than 8 hours with VP approval.

                Hotels. The hotel limit is $180 per night in tier-1 cities and $120 per night
                elsewhere. Room service is not reimbursed.

                Daily allowance. The per diem is $40 per day for domestic travel and $70 per day
                for international travel. Receipts are required for any single expense above $25.

                Expenses. Submit all expense claims within 14 days of returning from the trip.
                Late claims may be rejected.

                Extensions. Personal travel extensions are allowed, but the extra days are not
                reimbursed and personal insurance is the employee's responsibility.

                Insurance. Company travel insurance is provided automatically for international
                trips. Domestic trips are covered by the standard company policy.
                """, riya);
    }

    private User user(String name, String email, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(PASSWORD));
        user.setRole(role);
        return users.save(user);
    }
}
