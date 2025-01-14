/**
 * Preview URL for unpublished datasets.
 * <p>
 * The Preview (formerly Private) URL feature has been implemented as a specialized role assignment
 * with an associated token that permits read-only access to the metadata and
 * all files (regardless of if the files are restricted or not) of a draft
 * version of a dataset.
 * <p>
 * As of this note, a second option - to create a Preview URL that provides an
 * anonymized view of the dataset has been added. This option works the same as
 * the original except that it hides author names in the citation block, hides
 * the values for an admin specified list of metadata fields, disables citation
 * downloads, and disables API access (except for file and file thumbnail
 * downloads which are used by the UI).
 * <p>
 * The primary use case for a Preview URL is for journal editors to send a link
 * to reviewers of a dataset before publication. In most cases, these journal
 * editors do not permit depositors to publish on their own, which is to say
 * they only allow depositors to have the "Contributor" role on the datasets
 * they create. With only the "Contributor" role, depositors are unable to grant
 * even read-only access to any user within the Dataverse installation and must
 * contact the journal editor to make any adjustments to permissions, which they
 * can't even see. This is all by design because it is the journal editor, not
 * the depositor, who is in charge of both the security of the dataset and the
 * timing of when the dataset is published.
 * <p>
 * A secondary use case for a Preview URL is for depositors who have the ability
 * to manage permissions on their dataset (depositors who have the "Curator" or
 * "Admin" role, which grants much more power than the "Contributor" role) to
 * send a link to coauthors or other trusted parties to preview the dataset
 * before the depositors publish the dataset on their own. For better security,
 * these depositors could ask their coauthors to create Dataverse accounts and
 * assign roles to them directly, rather than using a Preview URL which requires
 * no username or password.
 * <p>
 * As of this note, a second option aimed specifically at the review use case -
 * to create a Preview URL that provides an anonymized view of the dataset - has
 * been added. This option works the same as the original except that it hides
 * author names in the citation block, hides the values for an admin specified
 * list of metadata fields, disables citation downloads, and disables API access
 * (except for file and file thumbnail downloads which are used by the UI).
 * <p>
 * The token associated with the Preview URL role assignment that can be used
 * either in the GUI or, for the non-anonymized-access option, via the API to
 * elevate privileges beyond what a "Guest" can see. The ability to use a
 * Preview URL token via API was added mostly to facilitate automated testing of
 * the feature but the far more common case is expected to be use of the Preview
 * URL token in a link that is clicked to open a browser, similar to links
 * shared via Dropbox, Google, etc.
 * <p>
 * When reviewers click a Preview URL their browser sessions are set to the
 * "{@link edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser}" that
 * has the "Member" role only on the dataset in question and redirected to that
 * dataset, where they will see an indication in blue at the top of the page
 * that they are viewing an unpublished dataset. If the reviewer happens to be
 * logged into Dataverse already, clicking the link will log them out because
 * the review is meant to be blind. Because the dataset is always in draft when
 * a Preview URL is in effect, no downloads or any other activity by the
 * reviewer are logged to the guestbook. All reviewers click the same Preview
 * URL containing the same token, and with the exception of an IP address being
 * logged, it should be impossible to trace which reviewers have clicked a
 * Preview URL. If the reviewer navigates to the home page, the session is set
 * to the Guest user and they will see what a Guest would see.
 * <p>
 * The "Member" role is used because it contains the necessary read-only
 * permissions, which are ViewUnpublishedDataset and DownloadFile. (Technically,
 * the "Member" role also has the ViewUnpublishedDataverse permission but
 * because the role is assigned at the dataset level and dataverses cannot be
 * children of datasets, this permission has no effect.) Reusing the "Member"
 * role helps contain the list of roles available at the dataset level to a
 * reasonable number (five).
 * <p>
 * Because the PrivateUrlUser has the "Member" role, all the same permissions
 * apply. This means that the PrivateUrlUser (the reviewer, typically) can
 * download all files, even if they have been restricted, across any dataset
 * version. A Member can also download restricted files that have been deleted
 * from previously published versions.
 * <p>
 * Likewise, when a Preview URL token is used via API, commands are executed
 * using the "PrivateUrlUser" that has the "Member" role only on the dataset in
 * question. This means that read-only operations such as downloads of the
 * dataset's files are permitted. The Search API does not respect the Preview
 * URL token but you can download files using the Access API, and, with the
 * non-anonymized-access option, download unpublished metadata using the Native
 * API.
 * <p>
 * A Preview URL cannot be created for a published version of a dataset. In the
 * GUI, you will be reminded of this fact with a popup. The API will explain
 * this as well.
 * <p>
 * An anonymized-access Preview URL can't be created if any published dataset
 * version exists. The primary reason for this is that, since datasets have
 * DOIs, the full metadata about published versions is available directly from
 * the DOI provider. (While the metadata for that version could be somewhat
 * different, in practice it would probably provide a means of identifying
 * some/all of the authors).
 * <p>
 * If a draft dataset containing a Preview URL is
 * published, the Preview URL is deleted. This means that reviewers who click
 * the link after publication will see a 404.
 * <p>
 * If a post-publication draft containing a Preview URL is deleted, the Preview
 * URL is deleted. This is to ensure that if a new draft is created in the
 * future, a new token will be used.
 * <p>
 * The creation and deletion of a Preview URL are limited to the "Curator" and
 * "Admin" roles because only those roles have the permission called
 * "ManageDatasetPermissions", which is the permission used by the
 * "AssignRoleCommand" and "RevokeRoleCommand" commands. If you have the
 * permission to create or delete a Preview URL, the fact that a Preview URL is
 * enabled for a dataset will be indicated in blue at the top of the page.
 * Success messages are shown at the top of the page when you create or delete a
 * Preview URL. In the GUI, deleting a Preview URL is called "disabling" and you
 * will be prompted for a confirmation. No matter what you call it the role is
 * revoked. You can also delete a Preview URL by revoking the role.
 * <p>
 * A "Contributor" does not have the "ManageDatasetPermissions" permission and
 * cannot see "Permissions" nor "Preview URL" under the "Edit" menu of their
 * dataset. When a Curator or Admin has enabled a Preview URL on a Contributor's
 * dataset, the Contributor does not see a visual indication that a Preview URL
 * has been enabled for their dataset.
 * <p>
 * There is no way for an "Admin" or "Curator" to see when a Preview URL was
 * created or deleted for a dataset but someone who has access to the database
 * can see that the following commands are logged to the "actionlogrecord"
 * database table:
 * <ul>
 * <li>{@link edu.harvard.iq.dataverse.engine.command.impl.GetPrivateUrlCommand}</li>
 * <li>{@link edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommand}</li>
 * <li>{@link edu.harvard.iq.dataverse.engine.command.impl.DeletePrivateUrlCommand}</li>
 * </ul>
 * See also the Preview URL To Unpublished Dataset BRD at <a href=
 * "https://docs.google.com/document/d/1FT47QkZKcmjSgRnePaJO2g1nzcotLyN3Yb2ORvBr6cs/edit?usp=sharing">
 * https://docs.google.com/document/d/1FT47QkZKcmjSgRnePaJO2g1nzcotLyN3Yb2ORvBr6cs/edit?usp=sharing</a>
 */
package edu.harvard.iq.dataverse.privateurl;
