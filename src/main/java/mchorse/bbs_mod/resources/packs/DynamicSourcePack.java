package mchorse.bbs_mod.resources.packs;

import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class DynamicSourcePack implements ISourcePack
{
    private ISourcePack main;
    private volatile ISourcePack secondary;

    public DynamicSourcePack(ISourcePack main)
    {
        this.main = main;
    }

    public void setSecondary(ISourcePack secondary)
    {
        this.secondary = secondary;
    }

    public ISourcePack getSourcePack()
    {
        return this.main;
    }

    public ISourcePack getSecondary()
    {
        return this.secondary;
    }

    @Override
    public String getPrefix()
    {
        return this.getSourcePack().getPrefix();
    }

    @Override
    public boolean hasAsset(Link link)
    {
        ISourcePack secondary = this.secondary;

        return (secondary != null && secondary.hasAsset(link)) || this.main.hasAsset(link);
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        ISourcePack secondary = this.secondary;

        return secondary != null && secondary.hasAsset(link) ? secondary.getAsset(link) : this.main.getAsset(link);
    }

    @Override
    public File getFile(Link link)
    {
        ISourcePack secondary = this.secondary;
        File local = secondary == null ? null : secondary.getFile(link);
        File shared = this.main.getFile(link);

        /* Merged folders use the main library for browsing and creating resources. */
        if (shared != null && shared.isDirectory() && (local == null || !local.isFile()))
        {
            return shared;
        }

        /* Existing world files stay editable in place; new files belong to the main library. */
        if (local != null && local.exists())
        {
            return local;
        }

        return shared;
    }

    @Override
    public Link getLink(File file)
    {
        ISourcePack secondary = this.secondary;
        Link link = secondary == null ? null : secondary.getLink(file);

        return link == null ? this.main.getLink(file) : link;
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
        ISourcePack secondary = this.secondary;

        if (secondary != null)
        {
            secondary.getLinksFromPath(links, link, recursive);
        }

        this.main.getLinksFromPath(links, link, recursive);
    }
}
