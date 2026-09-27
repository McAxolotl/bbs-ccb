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
        return this.secondary == null ? this.main : this.secondary;
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

        /* Existing files stay editable where they were loaded; new files belong to the world. */
        if (local != null && local.exists())
        {
            return local;
        }

        return shared != null && shared.exists() ? shared : (local == null ? shared : local);
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
